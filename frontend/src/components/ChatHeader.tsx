import { Link } from 'react-router-dom'
import type { Person } from '../lib/chat'
import Avatar from './Avatar'

type ChatHeaderProps = {
  /** The person on the other side of the conversation. */
  other: Person
  /** The signed-in account. Undefined only for the moment before it loads. */
  me?: Person
}

/**
 * The bar across the top of a chat: who you are talking to on the left, who
 * you are on the right.
 *
 * The right-hand side matters more than it looks. Inside a conversation there
 * is otherwise nothing to tell two signed-in accounts apart — both show the
 * other person's name — so it is easy to forget who you are and wonder why your
 * messages are on the wrong side.
 */
function ChatHeader({ other, me }: ChatHeaderProps) {
  return (
    <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-6 py-4">
      <div className="flex min-w-0 items-center gap-3">
        <Link to="/people" className="text-sm font-medium text-slate-500 hover:text-slate-900">
          Back
        </Link>
        <Avatar name={other.username} />
        <h1 className="truncate text-base font-semibold text-slate-900">{other.username}</h1>
      </div>

      {me && (
        <div className="flex shrink-0 items-center gap-2">
          <span className="hidden text-xs text-slate-500 sm:inline">Signed in as</span>
          <span className="text-sm font-medium text-slate-700">{me.username}</span>
          <Avatar name={me.username} self />
        </div>
      )}
    </header>
  )
}

export default ChatHeader
