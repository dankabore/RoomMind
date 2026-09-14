import { useState } from 'react'
import type { FormEvent } from 'react'

type MessageComposerProps = {
  /** Whose name goes in the placeholder. */
  recipient: string
  sending: boolean
  /** Why the last send failed. Undefined means it did not. */
  error?: string
  /** Resolves once the message is saved, and rejects if it was not. */
  onSend: (body: string) => Promise<unknown>
}

/**
 * The box along the bottom of a chat.
 *
 * It keeps the half-typed message to itself. When that text lived in the page,
 * every keystroke redrew the whole conversation above it; held here, typing
 * redraws only this box.
 */
function MessageComposer({ recipient, sending, error, onSend }: MessageComposerProps) {
  const [draft, setDraft] = useState('')

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const body = draft.trim()
    if (!body || sending) {
      return
    }

    try {
      await onSend(body)
      // Cleared only once the message is saved, so a failed send leaves the
      // text where it was to try again, instead of losing it.
      setDraft('')
    } catch {
      // Nothing to do here: the failure reaches the screen through `error`.
    }
  }

  return (
    <form onSubmit={handleSubmit} className="border-t border-slate-200 bg-white px-6 py-4">
      <div className="mx-auto flex w-full max-w-2xl gap-2">
        <input
          type="text"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder={`Message ${recipient}`}
          aria-label="Message"
          maxLength={4000}
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-slate-900 outline-none focus:border-slate-900"
        />
        <button
          type="submit"
          // A message of nothing but spaces is not worth sending, and the
          // backend would reject it anyway.
          disabled={draft.trim() === '' || sending}
          className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:bg-slate-300"
        >
          {sending ? 'Sending…' : 'Send'}
        </button>
      </div>

      {error && <p className="mx-auto mt-2 w-full max-w-2xl text-xs text-red-600">{error}</p>}
    </form>
  )
}

export default MessageComposer
