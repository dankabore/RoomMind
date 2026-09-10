import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
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
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto w-full max-w-2xl">
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

        <div className="mt-4 rounded-xl bg-white p-2 shadow">
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
                <li
                  key={person.id}
                  className="flex items-center gap-3 rounded-lg px-4 py-3 hover:bg-slate-50"
                >
                  {/* Stands in for a profile picture until there is one to show. */}
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-200 text-sm font-medium uppercase text-slate-600">
                    {person.username.charAt(0)}
                  </span>
                  <span className="text-sm font-medium text-slate-900">{person.username}</span>
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
