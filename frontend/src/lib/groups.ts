import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'

/**
 * Everything the group screens ask of the backend: making a group and changing
 * who is in it. Reading and sending a group's messages is no different from any
 * other conversation, so that stays in lib/chat.
 */

export type MemberRole = 'ADMIN' | 'MEMBER'

export type GroupMember = {
  id: number
  username: string
  role: MemberRole
}

export type Group = {
  id: number
  name: string
  members: GroupMember[]
}

// The group and its member list are the same cache entry, because the backend
// answers with both together and every change hands the whole thing back.
function groupKey(conversationId: number) {
  return ['group', conversationId]
}

/**
 * One group and who is in it. Any member may read this; the backend answers 404
 * to anyone who is not in the group, the same as it does for an id that never
 * existed.
 */
export function useGroup(conversationId: number) {
  return useQuery({
    queryKey: groupKey(conversationId),
    queryFn: async () => {
      const response = await api.get<Group>(`/api/conversations/${conversationId}/members`)
      return response.data
    },
    enabled: Number.isInteger(conversationId),
  })
}

/**
 * Makes a group with you as its admin. The people chosen here are in it from
 * the start; anyone else has to be added afterwards, which only the admin can
 * do.
 *
 * The conversation list is thrown away rather than updated, so the next visit to
 * the home page asks again and finds the new group. It is on that list from the
 * moment it is made, even with nothing said in it.
 */
export function useCreateGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (group: { name: string; memberIds: number[] }) => {
      const response = await api.post<Group>('/api/conversations/groups', group)
      return response.data
    },
    onSuccess: (group) => {
      queryClient.setQueryData(groupKey(group.id), group)
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
    },
  })
}

/**
 * Adds someone to a group. Admin only — an ordinary member's attempt is refused
 * by the backend, and the screen never offers them the button.
 *
 * Both this and the removal below answer with the group as it now stands, or
 * ask for it again, rather than editing the cached list by hand. The member list
 * is short and changes rarely, so the simpler version costs nothing worth
 * saving.
 */
export function useAddMember(conversationId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (userId: number) => {
      const response = await api.post<Group>(`/api/conversations/${conversationId}/members`, {
        userId,
      })
      return response.data
    },
    onSuccess: (group) => queryClient.setQueryData(groupKey(conversationId), group),
  })
}

/**
 * Takes someone out of a group. Admin only, and never yourself: to leave you
 * hand the role on first and then use the leave hook below.
 *
 * Removal answers with nothing at all — there is no group left to describe from
 * the removed person's side and the path already says who went — so this one
 * asks for the group again instead of being handed it.
 */
export function useRemoveMember(conversationId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (userId: number) => {
      await api.delete(`/api/conversations/${conversationId}/members/${userId}`)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupKey(conversationId) }),
  })
}

/**
 * Hands the admin role to another member. The admin who does it becomes an
 * ordinary member at the same moment, since a group has exactly one admin.
 *
 * The answer is the group with both roles already changed, so the panel redraws
 * from it and the buttons that only the admin has disappear on their own.
 */
export function useTransferAdmin(conversationId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (userId: number) => {
      const response = await api.put<Group>(`/api/conversations/${conversationId}/admin`, { userId })
      return response.data
    },
    onSuccess: (group) => queryClient.setQueryData(groupKey(conversationId), group),
  })
}

/**
 * Leaves a group. Refused for an admin who still has company — they have to
 * hand the role on first — and if the admin is the last one in, the group ends
 * with them.
 *
 * Afterwards the group is dropped from the cache rather than refetched: asking
 * for it again would be asking about a conversation the caller is no longer in,
 * which answers 404. The screen navigates away.
 */
export function useLeaveGroup(conversationId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async () => {
      await api.post(`/api/conversations/${conversationId}/leave`)
    },
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: groupKey(conversationId) })
      queryClient.removeQueries({ queryKey: ['messages', conversationId] })
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
    },
  })
}
