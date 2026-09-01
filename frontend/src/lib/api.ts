import axios from 'axios'
import { clearToken, getToken } from './auth'

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

// Signing in is allowed to fail with a 401 — that is just a wrong password, and
// the login form has its own message for it. Only those two endpoints are
// exempt; a 401 from anywhere else means the token we sent was not accepted.
const SIGN_IN_PATHS = ['/api/auth/login', '/api/auth/register']

/**
 * Runs after every response and catches the case the route guard cannot: a
 * token that was fine when the page loaded but has expired while the person was
 * sitting on it. Any later call then comes back 401, and rather than let the
 * page show an error it will keep showing, we throw the dead token away and
 * send them to the login page.
 *
 * The redirect is a plain browser navigation rather than React Router's, because
 * this file is not a component and has no access to the router. Leaving the page
 * this way also empties the cached query answers, which is what we want — none
 * of them should survive into the next person's session.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const url = error.config?.url ?? ''
      const isSignIn = SIGN_IN_PATHS.some((path) => url.includes(path))
      // Already on the login page means there is nothing to send them back
      // from, and navigating there again would only reload it in a loop.
      const alreadyThere = window.location.pathname === '/login'

      if (!isSignIn && !alreadyThere) {
        clearToken()
        window.location.assign('/login')
      }
    }

    // Hand the error on regardless, so the call that made the request still
    // sees it and its own catch block runs.
    return Promise.reject(error)
  },
)

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
