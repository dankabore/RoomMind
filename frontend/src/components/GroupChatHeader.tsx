import { Link } from 'react-router-dom'
import type { Person } from '../lib/chat'
import type { Group } from '../lib/groups'
import Avatar from './Avatar'

type GroupChatHeaderProps = {
  group: Group
  /** The signed-in account. Undefined only for the moment before it loads. */
  me?: Person
  /** Whether the member list is open, so the button can say what it will do. */
  membersOpen: boolean
  onToggleMembers: () => void
}

/**
 * The bar across the top of a group: the group's name and how many people are
 * in it on the left, the button that opens the member list and who you are on
 * the right.
 *
 * Separate from ChatHeader rather than a version of it with optional bits. A
 * group names itself and has a member count and a list to open; a chat with one
 * person has a name to show and nothing to open. The two only look alike.
 */
function GroupChatHeader({ group, me, membersOpen, onToggleMembers }: GroupChatHeaderProps) {
  const count = group.members.length

  return (
    <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-6 py-4">
      <div className="flex min-w-0 items-center gap-3">
        <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-900">
          Back
        </Link>
        <Avatar name={group.name} />
        <div className="min-w-0">
          <h1 className="truncate text-base font-semibold text-slate-900">{group.name}</h1>
          <p className="text-xs text-slate-500">
            {count} {count === 1 ? 'member' : 'members'}
          </p>
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-3">
        <button
          type="button"
          onClick={onToggleMembers}
          // The button controls a panel elsewhere on the screen, so a screen
          // reader is told what it opens and whether it is open already.
          aria-expanded={membersOpen}
          aria-controls="group-members"
          className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          {membersOpen ? 'Hide members' : 'Members'}
        </button>

        {me && (
          <div className="flex shrink-0 items-center gap-2">
            <span className="hidden text-xs text-slate-500 sm:inline">Signed in as</span>
            <span className="text-sm font-medium text-slate-700">{me.username}</span>
            <Avatar name={me.username} self />
          </div>
        )}
      </div>
    </header>
  )
}

export default GroupChatHeader
