import { useState } from 'react'
import type { Person } from '../lib/chat'
import type { GroupMember } from '../lib/groups'
import Avatar from './Avatar'
import ConfirmDialog from './ConfirmDialog'
import PersonPicker from './PersonPicker'

type GroupMembersProps = {
  members: GroupMember[]
  /** The reader's own id, to mark their row and to keep them off their own buttons. */
  myId?: number
  /** Whether the reader is the admin. Only then is the membership editable. */
  canManage: boolean
  onAdd: (userId: number) => Promise<unknown>
  /** Who is being added right now, if anyone. */
  addingId?: number
  addError?: string
  onRemove: (userId: number) => Promise<unknown>
  /** Who is being removed right now, if anyone. */
  removingId?: number
  removeError?: string
  onPromote: (userId: number) => Promise<unknown>
  /** Who is being made admin right now, if anyone. */
  promotingId?: number
  promoteError?: string
  onLeave: () => Promise<unknown>
  leaving: boolean
  leaveError?: string
}

/** The question currently being asked, if any. */
type Pending =
  | { kind: 'remove'; member: GroupMember }
  | { kind: 'promote'; member: GroupMember }
  | { kind: 'leave' }

/**
 * Who is in the group, down the side of the chat.
 *
 * Everybody sees the list — you can see who is writing in the group anyway —
 * and everybody can leave. Only the admin gets the buttons that change other
 * people's membership, which is also what the backend enforces: an ordinary
 * member's attempt is refused there, not just hidden here.
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
  onPromote,
  promotingId,
  promoteError,
  onLeave,
  leaving,
  leaveError,
}: GroupMembersProps) {
  const memberIds = members.map((member) => member.id)
  // Everything here that cannot be undone from this panel asks first, the same
  // way logging out does.
  const [pending, setPending] = useState<Pending | null>(null)

  // An admin cannot walk out on a group that still has people in it: somebody
  // has to be able to add and remove. The last one in may leave, and the group
  // goes with them.
  const alone = members.length === 1
  const mustHandOver = canManage && !alone

  async function run(action: () => Promise<unknown>) {
    try {
      await action()
    } catch {
      // Nothing to do here: each failure reaches the screen through its own
      // error message below.
    } finally {
      // Closed either way, because those messages sit behind this dialog.
      setPending(null)
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
      {promoteError && <p className="mt-2 text-sm text-red-600">{promoteError}</p>}

      <ul className="mt-2">
        {members.map((member) => {
          const isMe = member.id === myId
          // Nothing the admin can do to their own row: handing the role over
          // names somebody else, and leaving is the button at the foot of the
          // panel rather than removing yourself.
          const actionable = canManage && !isMe
          const busy = removingId !== undefined || promotingId !== undefined

          return (
            <li key={member.id} className="flex items-start gap-3 rounded-lg px-1 py-2">
              <Avatar name={member.username} self={isMe} />

              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-900">
                  {member.username}
                  {isMe && <span className="text-slate-400"> (you)</span>}
                </p>
                {member.role === 'ADMIN' && <p className="text-xs text-slate-500">Admin</p>}

                {actionable && (
                  <div className="mt-1 flex gap-3">
                    <button
                      type="button"
                      onClick={() => setPending({ kind: 'promote', member })}
                      disabled={busy}
                      className="text-xs font-medium text-slate-500 hover:text-slate-900 disabled:opacity-50"
                    >
                      {promotingId === member.id ? 'Handing over…' : 'Make admin'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setPending({ kind: 'remove', member })}
                      disabled={busy}
                      aria-label={`Remove ${member.username} from the group`}
                      className="text-xs font-medium text-red-600 hover:text-red-700 disabled:opacity-50"
                    >
                      {removingId === member.id ? 'Removing…' : 'Remove'}
                    </button>
                  </div>
                )}
              </div>
            </li>
          )
        })}
      </ul>

      {canManage && (
        <div className="mt-6 border-t border-slate-200 pt-4">
          <h3 className="mb-2 text-sm font-semibold text-slate-900">Add someone</h3>
          <PersonPicker
            excludeIds={memberIds}
            onPick={(person: Person) => run(() => onAdd(person.id))}
            busyId={addingId}
            error={addError}
            actionLabel="Add"
          />
        </div>
      )}

      <div className="mt-6 border-t border-slate-200 pt-4">
        {leaveError && <p className="mb-2 text-sm text-red-600">{leaveError}</p>}

        {mustHandOver ? (
          // Said rather than offered as a button that would only be refused:
          // the way out is above this line, on somebody else's row.
          <p className="text-xs text-slate-500">
            You run this group. Make someone else admin before you leave.
          </p>
        ) : (
          <button
            type="button"
            onClick={() => setPending({ kind: 'leave' })}
            disabled={leaving}
            className="text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-50"
          >
            {leaving ? 'Leaving…' : 'Leave group'}
          </button>
        )}
      </div>

      {pending?.kind === 'remove' && (
        <ConfirmDialog
          title={`Remove ${pending.member.username}?`}
          confirmLabel="Remove"
          busyLabel="Removing…"
          busy={removingId !== undefined}
          onConfirm={() => run(() => onRemove(pending.member.id))}
          onCancel={() => setPending(null)}
        >
          They will lose access to this group and everything said in it. Their messages stay, and you
          can add them back later.
        </ConfirmDialog>
      )}

      {pending?.kind === 'promote' && (
        <ConfirmDialog
          title={`Make ${pending.member.username} the admin?`}
          confirmLabel="Hand over"
          busyLabel="Handing over…"
          busy={promotingId !== undefined}
          tone="normal"
          onConfirm={() => run(() => onPromote(pending.member.id))}
          onCancel={() => setPending(null)}
        >
          They will be able to add and remove people, and you will become an ordinary member. Only
          they can give the role back.
        </ConfirmDialog>
      )}

      {pending?.kind === 'leave' && (
        <ConfirmDialog
          title="Leave this group?"
          confirmLabel="Leave"
          busyLabel="Leaving…"
          busy={leaving}
          onConfirm={() => run(onLeave)}
          onCancel={() => setPending(null)}
        >
          {alone
            ? 'You are the last one in it, so the group and everything in it will be deleted. This cannot be undone.'
            : 'You will stop seeing this group and its messages. Only its admin can add you back.'}
        </ConfirmDialog>
      )}
    </aside>
  )
}

export default GroupMembers
