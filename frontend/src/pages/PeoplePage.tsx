import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Avatar from '../components/Avatar'
import { api, errorMessage } from '../lib/api'

type Person = {
  id: number
  username: string
}

/**
 * Everyone registered, searchable by the start of their username. RoomMind has
 * no friend requests, so this list is the whole of finding somebody: you look
 * them up here and message them.
 */
function PeoplePage() {
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounced(search, 250)

  const { data: people, isPending, isError, error } = useQuery({
    // The term is part of the key, so each search is remembered separately and
    // deleting a letter shows the previous result instantly instead of asking
    // the backend for something it just answered.
    queryKey: ['people', debouncedSearch],
    queryFn: async () => {
      const response = await api.get<Person[]>('/api/users', {
        // Left off entirely when empty, so the URL is /api/users rather than
        // /api/users?search= — the backend treats them the same, but the two
        // spellings would otherwise be two entries in the browser's cache.
        params: debouncedSearch ? { search: debouncedSearch } : undefined,
      })
      return response.data
    },
  })

  // A search that has been typed but not yet sent. Without this the list would
  // sit there looking like a finished answer to the old term for a quarter of a
  // second after every keystroke.
  const settling = search !== debouncedSearch

  return (
    // Exactly one screen tall, never taller. The heading and search box keep
    // their size and the list gets what is left, so a long list scrolls inside
    // its own box instead of pushing the search box off the top of the page.
    <div className="flex h-screen flex-col bg-slate-100 p-6">
      <div className="mx-auto flex min-h-0 w-full max-w-2xl flex-1 flex-col">
        <div className="flex items-baseline justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">People</h1>
            <p className="mt-1 text-sm text-slate-500">Everyone on RoomMind.</p>
          </div>
          <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-900">
            Back
          </Link>
        </div>

        <input
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search by username"
          // The list below is what changes when you type here, so a screen
          // reader is told where to look rather than left to notice.
          aria-label="Search people by username"
          className="mt-6 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-slate-900"
        />

        {/* min-h-0 is what lets this box shrink below the height of its
            contents; without it the list stretches the page as before. It still
            only grows as tall as it needs to, so three people make a short box. */}
        <div className="mt-4 min-h-0 overflow-y-auto rounded-xl bg-white p-2 shadow">
          {(isPending || settling) && <p className="p-4 text-sm text-slate-500">Loading…</p>}

          {isError && !settling && (
            <p className="p-4 text-sm text-red-600">
              {errorMessage(error, 'Could not load the list of people.')}
            </p>
          )}

          {people && !settling && people.length === 0 && (
            <p className="p-4 text-sm text-slate-500">
              {search
                ? `Nobody here has a username starting with “${search}”.`
                : 'Nobody else has registered yet.'}
            </p>
          )}

          {people && !settling && people.length > 0 && (
            <ul>
              {people.map((person) => (
                <li key={person.id}>
                  {/* The whole row is the link, so the target is as big as it
                      looks rather than just the name. The address names the
                      person; the conversation is found or started on arrival. */}
                  <Link
                    to={`/chat/${person.id}`}
                    className="flex items-center gap-3 rounded-lg px-4 py-3 hover:bg-slate-50"
                  >
                    <Avatar name={person.username} />
                    <span className="text-sm font-medium text-slate-900">{person.username}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}

/**
 * Holds a value back until it has stopped changing for `delay` milliseconds.
 * Typing "anna" is five keystrokes; without this it is also five requests, four
 * of which are answers nobody will read by the time they arrive.
 */
function useDebounced(value: string, delay: number): string {
  const [settled, setSettled] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), delay)
    // Every new keystroke cancels the timer the last one started, so the value
    // only lands once the typing actually stops.
    return () => clearTimeout(timer)
  }, [value, delay])

  return settled
}

export default PeoplePage
