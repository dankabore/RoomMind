import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Avatar from '../components/Avatar'
import LogoutButton from '../components/LogoutButton'
import { api, errorMessage } from '../lib/api'
import type { ConversationSummary, Person } from '../lib/chat'
import { useConversations } from '../lib/chat'

/**
 * The signed-in home page: your conversations, most recently active first.
 * People you have never written to are not here — you find them on the people
 * page, and they appear here once the first message is sent.
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
            {/* The only way to start a conversation is to pick someone, and
                the people page is where you pick them. */}
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
 * One conversation: who it is with, when it was last active, and the start of
 * the last message. The message comes back from the API whole; `truncate` cuts
 * it to however much fits on this row's one line.
 */
function ConversationRow({ conversation, myId }: { conversation: ConversationSummary; myId?: number }) {
  const { otherUser, lastMessage } = conversation
  // Without "You:", a row whose last message you sent reads as though the
  // other person said it.
  const mine = lastMessage.senderId === myId

  return (
    <li>
      {/* The chat address names the person, the same as from the people page,
          so both ways in land on the same screen. */}
      <Link
        to={`/chat/${otherUser.id}`}
        className="flex items-center gap-3 rounded-lg px-4 py-3 hover:bg-slate-50"
      >
        <Avatar name={otherUser.username} />

        {/* min-w-0 lets this column be narrower than its text, which is what
            gives `truncate` below a width to cut at. */}
        <div className="min-w-0 flex-1">
          <div className="flex items-baseline justify-between gap-3">
            <span className="truncate text-sm font-medium text-slate-900">{otherUser.username}</span>
            <span className="shrink-0 text-xs text-slate-400">{formatWhen(lastMessage.createdAt)}</span>
          </div>
          <p className="truncate text-sm text-slate-500">
            {mine && <span className="text-slate-400">You: </span>}
            {lastMessage.body}
          </p>
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
