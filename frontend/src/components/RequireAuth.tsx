import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Navigate } from 'react-router-dom'
import { api } from '../lib/api'
import { clearToken, getToken } from '../lib/auth'

/**
 * Wraps the pages that only make sense when signed in. Having a token is not
 * quite the same as being signed in — a stored token can have expired since the
 * last visit — so this asks the backend to confirm it before showing anything,
 * and throws it away if the answer is no.
 */
function RequireAuth({ children }: { children: ReactNode }) {
  const hasToken = getToken() !== null

  const { data: user, isPending, isError } = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const response = await api.get('/api/auth/me')
      return response.data
    },
    // With no token there is nothing to check; skip the request entirely.
    enabled: hasToken,
    // A rejected token is not going to be accepted on a second attempt.
    retry: false,
  })

  if (!hasToken) {
    return <Navigate to="/login" replace />
  }

  if (isError) {
    clearToken()
    return <Navigate to="/login" replace />
  }

  if (isPending || !user) {
    return <p className="p-6 text-sm text-slate-500">Loading…</p>
  }

  return <>{children}</>
}

export default RequireAuth
