import { z } from 'zod'

/**
 * What each form is allowed to contain, written once as a "schema" — a
 * description of the shape and rules, which zod can then check a value against.
 * Keeping them here rather than inside the pages means the rules are readable on
 * their own, and the request bodies are described in the same breath.
 *
 * The messages are the ones shown under the fields, so they are written as
 * sentences addressed to the person filling the form in.
 */

/**
 * Signing in. Only emptiness is checked: whether the email and password are
 * actually right is the backend's call, and it answers with one message for
 * both so the form cannot be used to discover which emails have accounts.
 */
export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email.'),
  password: z.string().min(1, 'Enter your password.'),
})

/**
 * Creating an account. The same limits the backend enforces, checked here so an
 * obvious mistake is answered immediately instead of after a round trip. The
 * backend still checks everything itself; this is a convenience, not the real gate.
 */
export const registerSchema = z.object({
  // Two rules on one field, so an empty box and a typo get different messages.
  // `pipe` runs the second check only if the first passed, which is what keeps
  // an empty box from being told it does not look like an email address.
  email: z
    .string()
    .trim()
    .min(1, 'Enter an email address.')
    // html5Email is the pattern browsers use for <input type="email">. It is
    // looser than zod's default on purpose: the only address that truly counts
    // as valid is one that accepts mail, so this catches the obvious typo and
    // leaves the real judgement to the backend.
    .pipe(z.email({ pattern: z.regexes.html5Email, error: 'That does not look like an email address.' })),

  username: z
    .string()
    .trim()
    .min(1, 'Choose a username.')
    .min(3, 'At least 3 characters.')
    .max(50, 'At most 50 characters.'),

  password: z
    .string()
    .min(1, 'Choose a password.')
    .min(8, 'At least 8 characters.')
    // 72 is BCrypt's own limit, past which it ignores the extra characters.
    .max(72, 'At most 72 characters.'),
})

/**
 * The request bodies, worked out from the schemas rather than typed a second
 * time by hand — so a rule and its type can never drift apart. These are the
 * checked values, which means the email and username arrive already trimmed.
 */
export type LoginValues = z.infer<typeof loginSchema>
export type RegisterValues = z.infer<typeof registerSchema>
