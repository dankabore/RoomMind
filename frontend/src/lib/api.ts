import axios from 'axios'
import { getToken } from './auth'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

// Runs before every request and adds the bearer token when we have one, so no
// individual call has to remember the Authorization header. Requests made
// while logged out simply go without it, which is what /register and /login want.
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Turns whatever axios threw into a sentence worth showing a user. The backend
 * sends RFC 9457 problem details, so a rejected request carries its reason in
 * `detail` — "That email is already registered." and the like.
 */
export function errorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const detail = (error.response?.data as { detail?: string } | undefined)?.detail
    if (detail) {
      return detail
    }
    // A request that gets no response at all looks the same to the browser
    // whether the backend is down or the answer was blocked for coming from an
    // origin the backend does not allow, so the message has to name both. The
    // usual cause of the second is the dev server landing on 5174 because
    // something else already holds 5173.
    if (!error.response) {
      return 'Could not reach the server. Check that the backend is running, and that this page is on http://localhost:5173.'
    }
  }
  return fallback
}
