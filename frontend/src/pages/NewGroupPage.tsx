import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Avatar from '../components/Avatar'
import FormMessage from '../components/FormMessage'
import PersonPicker from '../components/PersonPicker'
import { errorMessage } from '../lib/api'
import type { Person } from '../lib/chat'
import { useCreateGroup } from '../lib/groups'

/**
 * Making a group: give it a name, pick who starts in it.
 *
 * You are its admin from the moment it is created, and nobody else can add
 * themselves later, so this page and the member list on the group screen are
 * the only two ways anyone ever gets in.
 */
function NewGroupPage() {
  const navigate = useNavigate()
  const createGroup = useCreateGroup()

  const [name, setName] = useState('')
  // The whole person, not just the id, because the list below shows the names
  // back and asking the backend again for something already in hand would be
  // wasteful.
  const [chosen, setChosen] = useState<Person[]>([])
  const [nameError, setNameError] = useState<string>()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const trimmed = name.trim()
    if (trimmed === '') {
      setNameError('Give the group a name.')
      return
    }

    try {
      const group = await createGroup.mutateAsync({
        name: trimmed,
        memberIds: chosen.map((person) => person.id),
      })
      // Straight into the group, which is where you would be going anyway.
      //
      // replace, so this page is not left behind in the history: pressing Back
      // from the new group should reach the home page, not the form that just
      // made it — where pressing Create again would make a second group with
      // the same name.
      navigate(`/group/${group.id}`, { replace: true })
    } catch {
      // Nothing to do here: the failure is shown below, from the mutation.
    }
  }

  return (
    <div className="flex h-screen flex-col bg-slate-100 p-6">
      <div className="mx-auto flex min-h-0 w-full max-w-2xl flex-1 flex-col">
        <div className="flex items-baseline justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">New group</h1>
            <p className="mt-1 text-sm text-slate-500">You will be its admin.</p>
          </div>
          <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-900">
            Back
          </Link>
        </div>

        <form onSubmit={handleSubmit} noValidate className="mt-6 flex min-h-0 flex-1 flex-col">
          <label className="block">
            <span className="text-sm font-medium text-slate-700">Group name</span>
            <input
              type="text"
              value={name}
              onChange={(event) => {
                setName(event.target.value)
                setNameError(undefined)
              }}
              maxLength={100}
              placeholder="Give it a short name"
              aria-invalid={nameError !== undefined}
              className={[
                'mt-1 w-full rounded-lg border bg-white px-3 py-2 text-slate-900 outline-none',
                nameError ? 'border-red-400 focus:border-red-500' : 'border-slate-300 focus:border-slate-900',
              ].join(' ')}
            />
            {nameError && <span className="mt-1 block text-sm text-red-600">{nameError}</span>}
          </label>

          {chosen.length > 0 && (
            <ul className="mt-4 flex flex-wrap gap-2">
              {chosen.map((person) => (
                <li key={person.id}>
                  {/* The chip itself removes the person, so taking somebody
                      back out is in the same place as seeing that they are in. */}
                  <button
                    type="button"
                    onClick={() => setChosen((people) => people.filter((p) => p.id !== person.id))}
                    aria-label={`Remove ${person.username} from the new group`}
                    className="flex items-center gap-2 rounded-full bg-white py-1 pl-1 pr-3 text-sm text-slate-700 shadow-sm hover:bg-slate-50"
                  >
                    <Avatar name={person.username} />
                    <span className="max-w-[10rem] truncate font-medium">{person.username}</span>
                    <span aria-hidden className="text-slate-400">
                      ×
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          {/* min-h-0 lets this card shrink and scroll inside the page rather
              than pushing the button below the bottom of the screen, and the
              flex column makes the picker fill it. Without the column the card
              stretched to the full height while the list inside it stopped
              part of the way down, leaving the rest of the card blank. */}
          <div className="mt-4 flex min-h-0 flex-1 flex-col rounded-xl bg-white p-3 shadow">
            <PersonPicker
              excludeIds={chosen.map((person) => person.id)}
              onPick={(person) => setChosen((people) => [...people, person])}
              actionLabel="Add"
            />
          </div>

          {createGroup.error && (
            <div className="mt-4">
              <FormMessage tone="error">
                {errorMessage(createGroup.error, 'Could not create that group.')}
              </FormMessage>
            </div>
          )}

          <div className="mt-4 flex items-center gap-3">
            <button
              type="submit"
              disabled={createGroup.isPending}
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
            >
              {createGroup.isPending ? 'Creating…' : 'Create group'}
            </button>
            {/* Said plainly, because a group of one looks like a mistake
                otherwise — it is allowed, and people can be added later. */}
            <p className="text-sm text-slate-500">
              {chosen.length === 0
                ? 'Nobody else picked yet. You can add people afterwards.'
                : `${chosen.length} ${chosen.length === 1 ? 'person' : 'people'} to start with.`}
            </p>
          </div>
        </form>
      </div>
    </div>
  )
}

export default NewGroupPage
