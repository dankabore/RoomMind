import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { clearToken } from '../lib/auth'

/**
 * The log out button and the "are you sure?" question it asks first. Kept
 * together because the button is useless without the question and the question
 * has no reason to exist without the button.
 */
function LogoutButton() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState(false)

  // Logging out is entirely local: the token is thrown away so no later request
  // carries it. Nothing is sent to the backend, because a signed token is not
  // recorded there and there is no session to end.
  function handleLogout() {
    clearToken()
    // Otherwise the cached "me" answer, and now the conversation list, would
    // still be sitting there for the next person who signs in on this browser.
    queryClient.clear()
    navigate('/login', { replace: true })
  }

  return (
    <>
      {/* Grey until you reach for it, red once you do: the colour warns about
          the action without shouting from the corner of every screen. */}
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-500 transition-colors hover:border-red-300 hover:bg-red-50 hover:text-red-600"
      >
        Log out
      </button>

      {confirming && (
        // The dark backdrop covers the page and closes the question when
        // clicked, so there is always a way out that is not the red button.
        <div
          className="fixed inset-0 z-10 flex items-center justify-center bg-slate-900/40 p-6"
          onClick={() => setConfirming(false)}
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
                onClick={() => setConfirming(false)}
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
    </>
  )
}

export default LogoutButton
