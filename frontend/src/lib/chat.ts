import { useMemo } from 'react'
import type { InfiniteData } from '@tanstack/react-query'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'

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

// How the cache holds one conversation: a list of pages, each a list of
// messages, plus the cursor that fetched each page.
type MessagePages = InfiniteData<Message[], number | null>

// Reading and sending both touch the same cache entry, so its name is built in
// one place and the two cannot drift apart.
function messagesKey(conversationId: number) {
  return ['messages', conversationId]
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
    getNextPageParam: (lastPage) => (lastPage.length === PAGE_SIZE ? lastPage[0].id : undefined),
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
 * Sends a message, then drops it straight into the cache rather than reloading.
 * Refetching an infinite query re-requests every page it holds, which would be
 * the whole conversation on every send.
 */
export function useSendMessage(conversationId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (body: string) => {
      const response = await api.post<Message>(`/api/conversations/${conversationId}/messages`, {
        body,
      })
      return response.data
    },
    onSuccess: (message) => {
      queryClient.setQueryData<MessagePages>(messagesKey(conversationId), (current) => {
        if (!current) {
          return current
        }
        // Page one is the newest, and within a page the newest is last.
        const pages = current.pages.slice()
        pages[0] = [...pages[0], message]
        return { ...current, pages }
      })
    },
  })
}
