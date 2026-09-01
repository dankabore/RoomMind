import type { ReactNode } from 'react'

type TextFieldProps = {
  label: string
  type: 'text' | 'email' | 'password'
  value: string
  onChange: (value: string) => void
  /** The complaint to show under the field. Undefined means the field is fine. */
  error?: string
  /** Standing advice, such as a minimum length. Replaced by `error` when there is one. */
  hint?: ReactNode
  autoComplete?: string
  minLength?: number
  maxLength?: number
}

/**
 * A labelled input that knows how to look wrong. Both auth forms use it, so the
 * red border, the message underneath and the screen-reader hint are defined
 * once and cannot drift apart between pages.
 *
 * `onChange` hands over the value rather than the event: no caller has ever
 * needed the rest of the event, and it keeps the pages free of `event.target`.
 */
function TextField({
  label,
  type,
  value,
  onChange,
  error,
  hint,
  autoComplete,
  minLength,
  maxLength,
}: TextFieldProps) {
  const invalid = error !== undefined

  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-700">{label}</span>

      <input
        type={type}
        // The form sets noValidate, so this no longer blocks the submit. It
        // stays because it is what tells a screen reader the field is not optional.
        required
        autoComplete={autoComplete}
        minLength={minLength}
        maxLength={maxLength}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={invalid}
        className={[
          'mt-1 w-full rounded-lg border px-3 py-2 text-slate-900 outline-none',
          invalid ? 'border-red-500 focus:border-red-600' : 'border-slate-300 focus:border-slate-900',
        ].join(' ')}
      />

      {/* The hint turns into the complaint rather than sitting beside it, so a
          field never shows two lines saying much the same thing. */}
      {invalid ? (
        <span className="mt-1 block text-xs text-red-600">{error}</span>
      ) : (
        hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>
      )}
    </label>
  )
}

export default TextField
