import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthCard from '../components/AuthCard'
import FormMessage from '../components/FormMessage'
import SubmitButton from '../components/SubmitButton'
import TextField from '../components/TextField'
import { api, errorMessage } from '../lib/api'
import { fieldErrorsFrom, isBlank, useFieldErrors } from '../lib/forms'
import { registerSchema } from '../lib/schemas'

// The field names, read off the schema rather than listed again here, so a
// renamed rule cannot leave a stale name behind.
type Field = keyof typeof registerSchema.shape

/**
 * Creating an account. The backend deliberately does not hand back a token
 * here, so a successful registration sends the person to the login page rather
 * than straight into the app.
 */
function RegisterPage() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { errors, setErrors, clear } = useFieldErrors<Field>()

  // Whether the button is worth pressing. Only emptiness is checked here; the
  // length rules stay in the schema so that a too-short password can be
  // submitted and explained rather than silently refusing to go anywhere.
  const incomplete = isBlank(email) || isBlank(username) || password === ''

  async function handleSubmit(event: FormEvent) {
    // Without this the browser reloads the page on submit and the request is lost.
    event.preventDefault()
    setError(null)

    // safeParse checks what was typed against registerSchema and hands back the
    // verdict instead of throwing: either the accepted values, or a list of
    // everything wrong with them. What counts as wrong lives in lib/schemas.ts.
    const checked = registerSchema.safeParse({ email, username, password })
    if (!checked.success) {
      setErrors(fieldErrorsFrom<Field>(checked.error))
      return
    }
    setErrors({})

    setSubmitting(true)

    try {
      // checked.data, not the raw state, so the email and username go up trimmed.
      await api.post('/api/auth/register', checked.data)
      navigate('/login', { state: { justRegistered: true } })
    } catch (caught) {
      setError(errorMessage(caught, 'Could not create the account.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Create an account"
      subtitle="Your email signs you in; your username is what other people see."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-slate-900 underline">
            Sign in
          </Link>
        </>
      }
    >
      {/* noValidate hands the checking to validate() above, so the field
          styling runs instead of the browser's own popup. */}
      <form className="space-y-4" onSubmit={handleSubmit} noValidate>
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          error={errors.email}
          onChange={(value) => {
            setEmail(value)
            clear('email')
          }}
        />

        <TextField
          label="Username"
          type="text"
          autoComplete="username"
          minLength={3}
          maxLength={50}
          value={username}
          error={errors.username}
          onChange={(value) => {
            setUsername(value)
            clear('username')
          }}
        />

        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          minLength={8}
          // 72 is BCrypt's own limit, past which it ignores the extra characters.
          maxLength={72}
          value={password}
          error={errors.password}
          hint="At least 8 characters."
          onChange={(value) => {
            setPassword(value)
            clear('password')
          }}
        />

        {error && <FormMessage tone="error">{error}</FormMessage>}

        <SubmitButton submitting={submitting} incomplete={incomplete} busyLabel="Creating…">
          Create account
        </SubmitButton>
      </form>
    </AuthCard>
  )
}

export default RegisterPage
