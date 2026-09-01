import type { ReactNode } from 'react'

type FormMessageProps = {
  tone: 'error' | 'success'
  children: ReactNode
}

const TONES = {
  error: 'bg-red-50 text-red-700',
  success: 'bg-emerald-50 text-emerald-700',
} as const

/**
 * A whole-form message, as opposed to the complaint under a single field: the
 * backend refusing a login, or the note that an account was just created.
 */
function FormMessage({ tone, children }: FormMessageProps) {
  return <p className={`rounded-lg px-3 py-2 text-sm ${TONES[tone]}`}>{children}</p>
}

export default FormMessage
