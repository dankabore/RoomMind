import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, errorMessage } from '../lib/api'
import type { Person } from '../lib/chat'
import Avatar from './Avatar'

type PersonPickerProps = {
  /** People to leave out: those already chosen, or already in the group. */
  excludeIds: number[]
  onPick: (person: Person) => void
  /** Who is mid-request, so their row can say so and stop taking clicks. */
  busyId?: number
  /** Why the last pick failed. Undefined means it did not. */
  error?: string
  /** What the button on each row says. */
  actionLabel: string
}

/**
 * Search for someone and pick them. Used to choose who a new group starts with,
 * and to add somebody to a group that already exists.
 *
 * It asks for the whole list of people once and narrows it here as you type,
 * rather than searching on the backend for every keystroke the way the people
 * page does. That page is the full directory and has to scale; this is a short
 * list inside a panel, and filtering what is already in hand means no waiting
 * and no request per letter.
 */
function PersonPicker({ excludeIds, onPick, busyId, error, actionLabel }: PersonPickerProps) {
  const [search, setSearch] = useState('')

  // The same cache entry the people page fills, so opening this after visiting
  // that page usually needs no request at all.
  const { data: people, isPending, isError, error: loadError } = useQuery({
    queryKey: ['people', ''],
    queryFn: async () => {
      const response = await api.get<Person[]>('/api/users')
      return response.data
    },
  })

  const matches = useMemo(() => {
    const term = search.trim().toLowerCase()
    return (people ?? [])
      .filter((person) => !excludeIds.includes(person.id))
      .filter((person) => person.username.toLowerCase().startsWith(term))
  }, [people, excludeIds, search])

  return (
    // Fills whatever box it is put in: the search box keeps its size and the
    // list below takes the rest, scrolling inside itself. In a short box, such
    // as the member panel, that is just the list at its natural height.
    <div className="flex h-full min-h-0 flex-col">
      <input
        type="search"
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        placeholder="Search by username"
        aria-label="Search people by username"
        className="w-full shrink-0 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-900"
      />

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      <div className="mt-2 min-h-0 flex-1 overflow-y-auto">
        {isPending && <p className="p-3 text-sm text-slate-500">Loading…</p>}

        {isError && (
          <p className="p-3 text-sm text-red-600">
            {errorMessage(loadError, 'Could not load the list of people.')}
          </p>
        )}

        {people && matches.length === 0 && (
          <p className="p-3 text-sm text-slate-500">
            {search ? 'Nobody left to add by that name.' : 'Nobody left to add.'}
          </p>
        )}

        <ul>
          {matches.map((person) => (
            <li key={person.id}>
              <button
                type="button"
                onClick={() => onPick(person)}
                disabled={busyId !== undefined}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left hover:bg-slate-50 disabled:opacity-50"
              >
                <Avatar name={person.username} />
                <span className="flex-1 truncate text-sm font-medium text-slate-900">
                  {person.username}
                </span>
                <span className="shrink-0 text-xs font-medium text-slate-500">
                  {busyId === person.id ? 'Adding…' : actionLabel}
                </span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

export default PersonPicker
