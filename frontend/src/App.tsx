import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from './lib/api'
import { clearToken } from './lib/auth'

type Health = {
  service: string
  status: string
  database: string
}

type User = {
  id: number
  email: string
  username: string
  createdAt: string
}

function App() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirmingLogout, setConfirmingLogout] = useState(false)

  const { data, isPending, isError, error } = useQuery({
    queryKey: ['health'],
    queryFn: async () => {
      const response = await api.get<Health>('/api/health')
      return response.data
    },
  })

  // RequireAuth has already made this call before rendering the page, so the
  // same query key returns the answer it cached rather than asking again.
  const { data: user } = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const response = await api.get<User>('/api/auth/me')
      return response.data
    },
    retry: false,
  })

  // Logging out is entirely local: the token is thrown away so no later request
  // carries it. Nothing is sent to the backend, because a signed token is not
  // recorded there and there is no session to end.
  function handleLogout() {
    clearToken()
    // Otherwise the cached "me" answer would still be sitting there for the
    // next person who signs in on this browser.
    queryClient.clear()
    navigate('/login', { replace: true })
  }

  return (
    // relative is what the button below positions itself against; without it
    // the corner it lands in would be the whole browser window's, not the page's.
    <div className="relative min-h-screen bg-slate-100 flex items-center justify-center p-6">
      {/* Grey until you reach for it, red once you do: the colour warns about
          the action without shouting from the corner of every screen. */}
      <button
        type="button"
        onClick={() => setConfirmingLogout(true)}
        className="absolute right-6 top-6 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-500 transition-colors hover:border-red-300 hover:bg-red-50 hover:text-red-600"
      >
        Log out
      </button>

      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">RoomMind</h1>
          <p className="mt-1 text-sm text-slate-500">
            {user ? `Signed in as ${user.username}` : 'Phase 1 — the project runs locally'}
          </p>
        </div>

        <div className="mt-6 space-y-2 text-sm">
          {isPending && <p className="text-slate-500">Checking the backend…</p>}

          {isError && (
            <p className="text-red-600">
              Backend unreachable: {error instanceof Error ? error.message : 'unknown error'}
            </p>
          )}

          {data && (
            <>
              <Row label="Backend" value={data.status} ok={data.status === 'ok'} />
              <Row label="Database" value={data.database} ok={data.database === 'up'} />
            </>
          )}
        </div>
      </div>

      {confirmingLogout && (
        // The dark backdrop covers the page and closes the question when
        // clicked, so there is always a way out that is not the red button.
        <div
          className="fixed inset-0 flex items-center justify-center bg-slate-900/40 p-6"
          onClick={() => setConfirmingLogout(false)}
        >
          <div
            className="w-full max-w-sm rounded-xl bg-white p-6 shadow-lg"
            // Without this a click inside the box would reach the backdrop
            // above and close the very dialog being used.
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="text-lg font-semibold text-slate-900">Log out?</h2>
            <p className="mt-1 text-sm text-slate-500">
              You will need your email and password to sign back in.
            </p>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirmingLogout(false)}
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700"
              >
                Log out
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Row({ label, value, ok }: { label: string; value: string; ok: boolean }) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-3">
      <span className="text-slate-600">{label}</span>
      <span className={ok ? 'font-medium text-emerald-600' : 'font-medium text-red-600'}>{value}</span>
    </div>
  )
}

export default App
