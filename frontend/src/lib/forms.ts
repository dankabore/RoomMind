import { useState } from 'react'
import type { ZodError } from 'zod'

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
 * Turns what zod reports into the shape the fields above read.
 *
 * A rejected value comes back as a list of `issues`, each carrying the message
 * and the `path` to the field it belongs to — `['username']` here, since these
 * forms are one level deep. One field can collect several issues at once (an
 * empty username is both blank and too short), and only the first is kept,
 * because the checks are written in the order a person should be told about
 * them and two lines under one box would only compete.
 */
export function fieldErrorsFrom<Field extends string>(error: ZodError): FieldErrors<Field> {
  const found: FieldErrors<Field> = {}

  for (const issue of error.issues) {
    const field = issue.path[0] as Field | undefined
    if (field !== undefined && found[field] === undefined) {
      found[field] = issue.message
    }
  }

  return found
}
