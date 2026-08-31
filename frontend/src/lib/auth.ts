/**
 * Where the login token lives between page loads. localStorage survives a
 * refresh and a closed tab, which is why the app can still know who you are
 * when you come back; clearing it is the whole of logging out, because the
 * backend keeps no session to end.
 */

const TOKEN_KEY = 'roommind.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
