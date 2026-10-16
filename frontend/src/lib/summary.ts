import { useMutation } from '@tanstack/react-query'
import { api } from './api'

/**
 * Asking for a group's summary. Groups only: the backend refuses a direct
 * conversation, and the screen only offers the button in a group.
 */

export type Summary = {
  summary: string
}

/**
 * Brings the group's summary up to date and hands it back.
 *
 * A mutation rather than a query, although nothing on screen changes: each
 * press may cost a paid call to the AI service, so it should only ever happen
 * when someone presses the button. A query would also run on its own whenever
 * the page is opened or the window regains focus.
 *
 * Pressing it again is cheap when nothing new has been said, because the
 * backend hands back the summary it saved last time without asking the model.
 */
export function useSummary(conversationId: number) {
  return useMutation({
    mutationFn: async () => {
      const response = await api.post<Summary>(`/api/conversations/${conversationId}/summary`)
      return response.data
    },
  })
}
