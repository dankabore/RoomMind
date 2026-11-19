import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'

/**
 * Your own profile: what you would like to be called, and the language you
 * read. Translation reads the language on the backend, so the screen only ever
 * needs it here, to show and change it.
 */

/** The codes the backend accepts — the same list as its Language enum. */
export type Language = 'EN' | 'FR' | 'ES' | 'DE' | 'IT' | 'PT' | 'AR' | 'ZH'

/**
 * What the dropdown offers, in order. Each language is named in itself first,
 * because someone looking for the language they read is looking for the word
 * they know it by; the English name follows for everyone else.
 */
export const LANGUAGES: { code: Language; label: string }[] = [
  { code: 'EN', label: 'English' },
  { code: 'FR', label: 'Français (French)' },
  { code: 'ES', label: 'Español (Spanish)' },
  { code: 'DE', label: 'Deutsch (German)' },
  { code: 'IT', label: 'Italiano (Italian)' },
  { code: 'PT', label: 'Português (Portuguese)' },
  { code: 'AR', label: 'العربية (Arabic)' },
  { code: 'ZH', label: '中文 (Chinese)' },
]

export type Profile = {
  id: number
  email: string
  username: string
  createdAt: string
  /** Null when not set; the username stands in for it. */
  displayName: string | null
  language: Language
}

const PROFILE_KEY = ['profile']

export function useProfile() {
  return useQuery({
    queryKey: PROFILE_KEY,
    queryFn: async () => {
      const response = await api.get<Profile>('/api/users/me')
      return response.data
    },
  })
}

/**
 * Saves the profile as a whole: both fields are sent every time, and the
 * backend replaces what it had. A blank display name comes back as null.
 *
 * The answer is the saved profile, so it goes straight into the cache instead
 * of asking again. `me` (what RequireAuth checked on the way in) is the same
 * shape from the same table, so it is overwritten too and the two never
 * disagree.
 */
export function useUpdateProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (profile: { displayName: string; language: Language }) => {
      const response = await api.put<Profile>('/api/users/me', profile)
      return response.data
    },
    onSuccess: (saved) => {
      queryClient.setQueryData(PROFILE_KEY, saved)
      queryClient.setQueryData(['me'], saved)
    },
  })
}
