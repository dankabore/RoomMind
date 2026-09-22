import type { ReactNode } from 'react'

type ConfirmDialogProps = {
  title: string
  children: ReactNode
  /** What the confirm button says, and says instead while the request is running. */
  confirmLabel: string
  busyLabel: string
  busy: boolean
  /** Red for anything that takes something away; dark for the rest. */
  tone?: 'danger' | 'normal'
  onConfirm: () => void
  onCancel: () => void
}

const TONES = {
  danger: 'bg-red-600 hover:bg-red-700',
  normal: 'bg-slate-900 hover:bg-slate-700',
} as const

/**
 * The "are you sure?" question, in the shape the log out button established: a
 * dark backdrop that closes it when clicked, a heading, a sentence saying what
 * will happen, then Cancel beside the button that does it.
 *
 * Pulled out here once the group panel needed three of these — removing
 * somebody, handing over the admin role, and leaving. LogoutButton still has
 * its own copy; there was no reason to disturb it.
 */
function ConfirmDialog({
  title,
  children,
  confirmLabel,
  busyLabel,
  busy,
  tone = 'danger',
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    // The backdrop closes the question, so there is always a way out that is
    // not the confirm button.
    <div
      className="fixed inset-0 z-10 flex items-center justify-center bg-slate-900/40 p-6"
      onClick={onCancel}
    >
      <div
        className="w-full max-w-sm rounded-xl bg-white p-6 shadow-lg"
        // Without this a click inside the box would reach the backdrop above
        // and close the very dialog being used.
        onClick={(event) => event.stopPropagation()}
      >
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        <p className="mt-1 text-sm text-slate-500">{children}</p>

        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className={`rounded-lg px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50 ${TONES[tone]}`}
          >
            {busy ? busyLabel : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ConfirmDialog
