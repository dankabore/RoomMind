import type { ReactNode } from 'react'

type SubmitButtonProps = {
  submitting: boolean
  /** What the button says while the request is in flight. */
  busyLabel: string
  /** True while the form is not filled in enough to be worth sending. */
  incomplete?: boolean
  children: ReactNode
}

/**
 * The button at the foot of a form. It goes flat for two different reasons: a
 * request is already in flight, so a second click would send a second one, or
 * the form is not filled in yet and there is nothing worth sending.
 */
function SubmitButton({ submitting, busyLabel, incomplete = false, children }: SubmitButtonProps) {
  return (
    <button
      type="submit"
      disabled={submitting || incomplete}
      className="w-full rounded-lg bg-slate-900 px-4 py-2 font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-slate-900"
    >
      {submitting ? busyLabel : children}
    </button>
  )
}

export default SubmitButton
