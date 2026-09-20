import { useState } from 'react'
import type { Person } from '../lib/chat'
import type { GroupMember } from '../lib/groups'
import Avatar from './Avatar'
import PersonPicker from './PersonPicker'

type GroupMembersProps = {
  members: GroupMember[]
  /** The reader's own id, to mark their row and to keep them off their own remove button. */
  myId?: number
  /** Whether the reader is the admin. Only then is anything on this panel editable. */
  canManage: boolean
  onAdd: (userId: number) => Promise<unknown>
  /** Who is being added right now, if anyone. */
  addingId?: number
  addError?: string
  onRemove: (userId: number) => Promise<unknown>
  /** Who is being removed right now, if anyone. */
  removingId?: number
  removeError?: string
}

/**
 * Who is in the group, down the side of the chat.
 *
 * Everybody sees the list — you can see who is writing in the group anyway —
 * but only the admin gets the buttons. For everyone else this is a panel that
 * reads, which is also what the backend enforces: an ordinary member's attempt
 * to add or remove is refused there, not just hidden here.
 */
function GroupMembers({
  members,
  myId,
  canManage,
  onAdd,
  addingId,
  addError,
  onRemove,
  removingId,
  removeError,
}: GroupMembersProps) {
  const memberIds = members.map((member) => member.id)
  // Who the "are you sure?" question is currently about, or null when it is not
  // being asked. Removing somebody cannot be undone from here — the admin would
  // have to add them back — so it is asked the same way logging out is.
  const [confirming, setConfirming] = useState<GroupMember | null>(null)

  async function handlePick(person: Person) {
    try {
      await onAdd(person.id)
    } catch {
      // Nothing to do here: the failure reaches the screen through addError.
    }
  }

  async function handleRemove(member: GroupMember) {
    try {
      await onRemove(member.id)
    } catch {
      // Same again — removeError is what says so.
    } finally {
      // Closed either way. A failure has the panel's own message to show it,
      // and that message sits behind this dialog.
      setConfirming(null)
    }
  }

  return (
    // Its own scrolling column beside the messages, so a long member list never
    // makes the page itself scroll.
    <aside
      id="group-members"
      className="flex w-72 shrink-0 flex-col overflow-y-auto border-l border-slate-200 bg-white px-4 py-4"
    >
      <h2 className="text-sm font-semibold text-slate-900">Members</h2>

      {removeError && <p className="mt-2 text-sm text-red-600">{removeError}</p>}

      <ul className="mt-2">
        {members.map((member) => {
          const isMe = member.id === myId
          // The admin cannot remove themselves — that is leaving, which is a
          // different thing and is not built yet — so their own row has no
          // button rather than one that would be refused.
          const removable = canManage && !isMe

          return (
            <li key={member.id} className="flex items-center gap-3 rounded-lg px-1 py-2">
              <Avatar name={member.username} self={isMe} />

              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-900">
                  {member.username}
                  {isMe && <span className="text-slate-400"> (you)</span>}
                </p>
                {member.role === 'ADMIN' && <p className="text-xs text-slate-500">Admin</p>}
              </div>

              {removable && (
                <button
                  type="button"
                  onClick={() => setConfirming(member)}
                  disabled={removingId !== undefined}
                  aria-label={`Remove ${member.username} from the group`}
                  className="shrink-0 text-xs font-medium text-red-600 hover:text-red-700 disabled:opacity-50"
                >
                  {removingId === member.id ? 'Removing…' : 'Remove'}
                </button>
              )}
            </li>
          )
        })}
      </ul>

      {confirming && (
        // Same shape as the log out question: a backdrop that closes it, so
        // there is always a way out that is not the red button.
        <div
          className="fixed inset-0 z-10 flex items-center justify-center bg-slate-900/40 p-6"
          onClick={() => setConfirming(null)}
        >
          <div
            className="w-full max-w-sm rounded-xl bg-white p-6 shadow-lg"
            // Without this a click inside the box would reach the backdrop
            // above and close the very dialog being used.
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="text-lg font-semibold text-slate-900">
              Remove {confirming.username}?
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              They will lose access to this group and everything said in it.
              Their messages stay, and you can add them back later.
            </p>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirming(null)}
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => handleRemove(confirming)}
                disabled={removingId !== undefined}
                className="rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
              >
                {removingId === confirming.id ? 'Removing…' : 'Remove'}
              </button>
            </div>
          </div>
        </div>
      )}

      {canManage && (
        <div className="mt-6 border-t border-slate-200 pt-4">
          <h3 className="mb-2 text-sm font-semibold text-slate-900">Add someone</h3>
          <PersonPicker
            excludeIds={memberIds}
            onPick={handlePick}
            busyId={addingId}
            error={addError}
            actionLabel="Add"
          />
        </div>
      )}
    </aside>
  )
}

export default GroupMembers
