import { useEffect, useMemo } from 'react'
import type { InfiniteData, QueryClient } from '@tanstack/react-query'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import { subscribe } from './socket'

/**
 * Everything the chat screen asks of the backend. The components only draw;
 * fetching, caching and sending live here, so the rules about pages and
 * cursors are written down in one place.
 */

/** Must match the backend's page size, or "is there more?" below guesses wrong. */
const PAGE_SIZE = 50

export type Person = {
  id: number
  username: string
}

export type Conversation = {
  id: number
  otherUser: Person
}

export type Message = {
  id: number
  conversationId: number
  senderId: number
  senderUsername: string
  body: string
  createdAt: string
}

/**
 * One row of the dashboard: a conversation and the last thing said in it.
 *
 * A group with nothing said in it yet is on this list; a direct conversation in
 * that state is not, because opening someone's chat creates one whether or not
 * anything is said.
 *
 * A row is either a direct conversation or a group, and `type` says which. The
 * two name themselves differently — a direct conversation by the person on the
 * other side, a group by its own name — so exactly one of `otherUser` and
 * `name` is filled in.
 */
export type ConversationSummary = {
  id: number
  type: 'DIRECT' | 'GROUP'
  name: string | null
  otherUser: Person | null
  /** Null for a group nobody has written in yet. */
  lastMessage: Message | null
  /** When the conversation began; what an empty group shows instead of a message time. */
  createdAt: string
}

// How the cache holds one conversation: a list of pages, each a list of
// messages, plus the cursor that fetched each page.
type MessagePages = InfiniteData<Message[], number | null>

// Reading and sending both touch the same cache entry, so its name is built in
// one place and the two cannot drift apart.
function messagesKey(conversationId: number) {
  return ['messages', conversationId]
}

/**
 * Your conversations, most recently active first. The backend already leaves
 * out conversations that were opened but never written in, and already sorts
 * them, so the screen can draw the list as it arrives.
 *
 * Coming back to the dashboard from a chat asks for the list again, which is
 * what moves a conversation you just wrote in to the top.
 */
export function useConversations() {
  return useQuery({
    queryKey: ['conversations'],
    queryFn: async () => {
      const response = await api.get<ConversationSummary[]>('/api/conversations')
      return response.data
    },
  })
}

/**
 * Which conversation you share with this person.
 *
 * A POST behind useQuery looks odd, but this request only ever hands back the
 * same conversation for the same pair of people — asking twice creates nothing
 * the second time — so it behaves like a read and benefits from being cached
 * like one.
 */
export function useDirectConversation(otherUserId: number) {
  return useQuery({
    queryKey: ['conversation', 'direct', otherUserId],
    queryFn: async () => {
      const response = await api.post<Conversation>('/api/conversations/direct', {
        userId: otherUserId,
      })
      return response.data
    },
    enabled: Number.isInteger(otherUserId),
  })
}

/**
 * A conversation's messages, a page at a time, in reading order.
 *
 * "Next page" here means further back in time. The cursor is the id of the
 * oldest message already held, which is the first entry of the page fetched
 * last — each page arrives oldest-first. A short page means the top of the
 * conversation has been reached, so returning undefined stops the loading.
 */
export function useMessages(conversationId: number) {
  const query = useInfiniteQuery({
    queryKey: messagesKey(conversationId),
    queryFn: async ({ pageParam }) => {
      const response = await api.get<Message[]>(`/api/conversations/${conversationId}/messages`, {
        params: pageParam === null ? undefined : { before: pageParam },
      })
      return response.data
    },
    // Null rather than undefined: the first page has no cursor, and the cache
    // refuses to store undefined.
    initialPageParam: null as number | null,
    // Fifty or more rather than exactly fifty: when the only page held is also
    // the newest, messages sent or received since are added to it and it grows
    // past fifty. Testing for exactly fifty would then decide there is nothing
    // older, and scrolling up would stop working.
    getNextPageParam: (lastPage) => (lastPage.length >= PAGE_SIZE ? lastPage[0].id : undefined),
  })

  // Pages arrive newest-first — page one is the most recent fifty — while the
  // screen reads top to bottom, oldest first. Reversing the pages (but not the
  // messages inside them) puts the whole conversation in reading order.
  const messages = useMemo(
    () => (query.data ? [...query.data.pages].reverse().flat() : []),
    [query.data],
  )

  // Renamed from the library's page vocabulary into the chat's: "next page"
  // means "older messages", so the components never have to translate.
  return {
    messages,
    loading: query.isPending,
    error: query.error,
    hasOlder: query.hasNextPage,
    loadingOlder: query.isFetchingNextPage,
    loadOlder: query.fetchNextPage,
  }
}

/**
 * Puts one message into the cached conversation, unless it is already there.
 *
 * Every new message arrives this way, whether it came back from your own send
 * or was pushed over the live connection. Your own messages come both ways —
 * the backend pushes to everyone watching, you included — so the id check is
 * what stops them appearing twice. Whichever copy lands first is kept.
 *
 * Nothing is refetched: refetching an infinite query re-requests every page it
 * holds, which would be the whole conversation for each message.
 */
function addMessage(queryClient: QueryClient, message: Message) {
  queryClient.setQueryData<MessagePages>(messagesKey(message.conversationId), (current) => {
    if (!current || current.pages.some((page) => page.some((held) => held.id === message.id))) {
      return current
    }
    // Page one is the newest, and within a page the newest is last. Sorting by
    // id keeps that true if two messages arrive in the opposite order to the
    // one they were saved in.
    const pages = current.pages.slice()
    pages[0] = [...pages[0], message].sort((a, b) => a.id - b.id)
    return { ...current, pages }
  })
}

/**
 * Keeps an open conversation up to date as other people write in it: listens
 * on its live address while the screen is showing, and stops when it closes.
 *
 * After a dropped connection comes back, it also asks for the newest page and
 * adds whatever is not already on screen, since messages sent during the gap
 * were never pushed. More than fifty missed messages would leave a hole above
 * those fifty, until the chat is reopened.
 */
export function useLiveMessages(conversationId: number) {
  const queryClient = useQueryClient()

  useEffect(() => {
    return subscribe(
      `/topic/conversations/${conversationId}`,
      (body) => addMessage(queryClient, JSON.parse(body) as Message),
      async () => {
        // Nothing on screen yet means the first load is still under way, and it
        // will bring the latest messages itself.
        if (!queryClient.getQueryData(messagesKey(conversationId))) {
          return
        }
        try {
          const response = await api.get<Message[]>(`/api/conversations/${conversationId}/messages`)
          response.data.forEach((message) => addMessage(queryClient, message))
        } catch {
          // Left as it is. The next reconnect, or reopening the chat, tries again.
        }
      },
    )
  }, [conversationId, queryClient])
}

/** Sends a message and adds it to the conversation once the backend has saved it. */
export function useSendMessage(conversationId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (body: string) => {
      const response = await api.post<Message>(`/api/conversations/${conversationId}/messages`, {
        body,
      })
      return response.data
    },
    onSuccess: (message) => addMessage(queryClient, message),
  })
}
