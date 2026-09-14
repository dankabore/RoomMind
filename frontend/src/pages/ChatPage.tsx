import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import ChatHeader from '../components/ChatHeader'
import MessageComposer from '../components/MessageComposer'
import MessageList from '../components/MessageList'
import { api, errorMessage } from '../lib/api'
import type { Conversation, Person } from '../lib/chat'
import { useDirectConversation, useMessages, useSendMessage } from '../lib/chat'

/**
 * One conversation with one person.
 *
 * The address is /chat/<their user id>, not the conversation's id, because the
 * conversation may not exist yet — it is created the first time this page is
 * opened. Naming the person instead means the URL can be typed, bookmarked or
 * reloaded without the app having to already know which conversation it is.
 *
 * This part only works out which conversation that is. The screen itself is
 * ChatView below, drawn once the answer is in — so nothing inside it has to
 * cope with a conversation that might not exist yet.
 */
function ChatPage() {
  const { userId } = useParams()
  const otherUserId = Number(userId)
  const { data: conversation, isPending, error } = useDirectConversation(otherUserId)

  // This check has to come first. For an address like /chat/abc the lookup
  // never runs, and a lookup that never runs counts as pending forever.
  if (!Number.isInteger(otherUserId)) {
    return <Centered>That is not a person.</Centered>
  }

  if (isPending) {
    return <Centered>Opening the conversation…</Centered>
  }

  if (!conversation) {
    return <Centered>{errorMessage(error, 'Could not open that conversation.')}</Centered>
  }

  // The key starts a fresh ChatView for each conversation. Without it, moving
  // from one chat to another would carry over the half-typed message and the
  // scroll position from the last one.
  return <ChatView key={conversation.id} conversation={conversation} />
}

/**
 * The chat screen: header, messages, input box. It connects the data from
 * lib/chat to the components that draw it, and does nothing else.
 */
function ChatView({ conversation }: { conversation: Conversation }) {
  // Cached by RequireAuth before this page rendered, so this reads the answer
  // rather than asking again. It is here to know which messages are your own.
  const { data: me } = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const response = await api.get<Person>('/api/auth/me')
      return response.data
    },
    retry: false,
  })

  const history = useMessages(conversation.id)
  const sendMessage = useSendMessage(conversation.id)
  const otherUsername = conversation.otherUser.username

  return (
    // Exactly one screen tall: the header and the input box keep their size and
    // the message list takes whatever is left, so only the list scrolls.
    <div className="flex h-screen flex-col bg-slate-100">
      <ChatHeader other={conversation.otherUser} me={me} />

      <MessageList
        messages={history.messages}
        myId={me?.id}
        otherUsername={otherUsername}
        loading={history.loading}
        error={history.error ? errorMessage(history.error, 'Could not load these messages.') : undefined}
        hasOlder={history.hasOlder}
        loadingOlder={history.loadingOlder}
        onLoadOlder={history.loadOlder}
      />

      <MessageComposer
        recipient={otherUsername}
        sending={sendMessage.isPending}
        error={sendMessage.error ? errorMessage(sendMessage.error, 'That message did not send.') : undefined}
        onSend={sendMessage.mutateAsync}
      />
    </div>
  )
}

function Centered({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-6">
      <p className="text-sm text-slate-500">{children}</p>
    </div>
  )
}

export default ChatPage
