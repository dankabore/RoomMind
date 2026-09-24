import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Avatar from '../components/Avatar'
import LogoutButton from '../components/LogoutButton'
import { api, errorMessage } from '../lib/api'
import type { ConversationSummary, Person } from '../lib/chat'
import { useConversations } from '../lib/chat'

/**
 * The signed-in home page: your conversations, most recently active first —
 * people and groups in one list.
 *
 * Conversations nothing has been said in are not here. People you have never
 * written to are found on the people page; a group you have just made is
 * reached from the page that made it, and joins this list once somebody writes
 * in it.
 */
function DashboardPage() {
  // Cached by RequireAuth before this page rendered, so this reads the answer
  // rather than asking again.
  const { data: me } = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const response = await api.get<Person>('/api/auth/me')
      return response.data
    },
    retry: false,
  })

  const { data: conversations, isPending, isError, error } = useConversations()

  return (
    // Exactly one screen tall, like the people page: the heading stays put and
    // a long list scrolls inside its own box.
    <div className="flex h-screen flex-col bg-slate-100 p-6">
      <div className="mx-auto flex min-h-0 w-full max-w-2xl flex-1 flex-col">
        <div className="flex items-center justify-between gap-3">
          <div className="min-w-0">
            <h1 className="text-2xl font-semibold text-slate-900">Conversations</h1>
            <p className="mt-1 truncate text-sm text-slate-500">
              {me ? `Signed in as ${me.username}` : '\u00a0'}
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-2">
            {/* The two ways to start something: pick one person on the people
                page, or name a group and pick several. */}
            <Link
              to="/groups/new"
              className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              New group
            </Link>
            <Link
              to="/people"
              className="rounded-lg bg-slate-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-700"
            >
              New message
            </Link>
            <LogoutButton />
          </div>
        </div>

        <div className="mt-6 min-h-0 overflow-y-auto rounded-xl bg-white p-2 shadow">
          {isPending && <p className="p-4 text-sm text-slate-500">Loading…</p>}

          {isError && (
            <p className="p-4 text-sm text-red-600">
              {errorMessage(error, 'Could not load your conversations.')}
            </p>
          )}

          {conversations && conversations.length === 0 && (
            <p className="p-4 text-sm text-slate-500">
              No conversations yet.{' '}
              <Link to="/people" className="font-medium text-slate-900 underline">
                Find someone to message
              </Link>
              .
            </p>
          )}

          {conversations && conversations.length > 0 && (
            <ul>
              {conversations.map((conversation) => (
                <ConversationRow key={conversation.id} conversation={conversation} myId={me?.id} />
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}

/**
 * One conversation: what it is, when it was last active, and the start of the
 * last message. The message comes back from the API whole; `truncate` cuts it
 * to however much fits on this row's one line.
 *
 * A group and a direct conversation differ in three small ways — what names the
 * row, where it leads, and who the preview credits — so they share this one row
 * rather than having a component each.
 */
function ConversationRow({ conversation, myId }: { conversation: ConversationSummary; myId?: number }) {
  const { otherUser, lastMessage } = conversation
  // Without "You:", a row whose last message you sent reads as though somebody
  // else said it.
  const mine = lastMessage?.senderId === myId
  const group = conversation.type === 'GROUP'

  // The backend fills exactly one of these, but the screen should not fall over
  // if that ever stops being true.
  const title = group ? conversation.name ?? 'Group' : otherUser?.username ?? 'Unknown'
  // A direct chat is addressed by the person, since the conversation may not
  // exist until it is opened; a group already exists and is addressed by its
  // own id.
  const href = group ? `/group/${conversation.id}` : `/chat/${otherUser?.id}`

  return (
    <li>
      <Link
        to={href}
        className="flex items-center gap-3 rounded-lg px-4 py-3 hover:bg-slate-50"
      >
        <Avatar name={title} />

        {/* min-w-0 lets this column be narrower than its text, which is what
            gives `truncate` below a width to cut at. */}
        <div className="min-w-0 flex-1">
          <div className="flex items-baseline justify-between gap-3">
            <span className="truncate text-sm font-medium text-slate-900">{title}</span>
            {/* A group nobody has written in yet is dated by when it was
                made, which is the only thing that has happened to it. */}
            <span className="shrink-0 text-xs text-slate-400">
              {formatWhen(lastMessage?.createdAt ?? conversation.createdAt)}
            </span>
          </div>
          {lastMessage ? (
            <p className="truncate text-sm text-slate-500">
              {/* In a group, whoever spoke has to be named: several people write
                  there, and the row would otherwise say nothing about who. */}
              {mine ? (
                <span className="text-slate-400">You: </span>
              ) : (
                group && <span className="text-slate-400">{lastMessage.senderUsername}: </span>
              )}
              {lastMessage.body}
            </p>
          ) : (
            // A group you have just made. Only groups reach this: an unused
            // direct conversation is left off the list entirely.
            <p className="truncate text-sm italic text-slate-400">No messages yet</p>
          )}
        </div>
      </Link>
    </li>
  )
}

/**
 * How recent a message is, in the fewest words that answer it: a time for
 * today, "Yesterday", a date for anything older, and the year only once it is
 * not this one.
 */
function formatWhen(iso: string): string {
  const date = new Date(iso)
  const now = new Date()

  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return 'Yesterday'
  }

  return date.toLocaleDateString([], {
    month: 'short',
    day: 'numeric',
    year: date.getFullYear() === now.getFullYear() ? undefined : 'numeric',
  })
}

export default DashboardPage
