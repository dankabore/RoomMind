import { useMutation } from '@tanstack/react-query'
import { api } from './api'
import type { Language } from './profile'

/**
 * Translating one message into the reader's language. The backend picks the
 * language from the profile; the screen never says which.
 */

export type Translation = {
  messageId: number
  language: Language
  translation: string
}

/**
 * A mutation, like summaries: it runs only when Translate is pressed, never on
 * its own when a page opens or the window regains focus.
 *
 * The answer stays in the mutation's `data` for as long as the bubble is on
 * screen, so switching between the original and the translation does not ask
 * again. A bubble drawn afresh (the page reopened) does ask again, but the
 * backend has the translation saved by then and answers without calling the
 * model.
 */
export function useTranslateMessage(conversationId: number, messageId: number) {
  return useMutation({
    mutationFn: async () => {
      const response = await api.post<Translation>(
        `/api/conversations/${conversationId}/messages/${messageId}/translation`,
      )
      return response.data
    },
  })
}
