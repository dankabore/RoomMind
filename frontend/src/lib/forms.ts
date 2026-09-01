import { useState } from 'react'

/**
 * The complaints a form is currently showing, keyed by field name. Every key is
 * optional, so an empty object means "nothing wrong yet" and a missing key means
 * that one field is fine.
 */
export type FieldErrors<Field extends string> = Partial<Record<Field, string>>

/**
 * Holds those complaints for one form. The `clear` half is why this exists:
 * every field needs to drop its own message as soon as it is typed in, and
 * writing that spread by hand at each input is where mistakes creep in.
 */
export function useFieldErrors<Field extends string>() {
  const [errors, setErrors] = useState<FieldErrors<Field>>({})

  function clear(field: Field) {
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  return { errors, setErrors, clear }
}

export function isBlank(value: string): boolean {
  return value.trim() === ''
}

/**
 * Deliberately loose. The only address that truly counts as valid is one that
 * accepts mail, so this catches the obvious typo and leaves the real judgement
 * to the backend rather than pretending a regular expression can decide it.
 */
export function looksLikeEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
}
