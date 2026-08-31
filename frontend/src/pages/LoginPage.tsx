import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import AuthCard from '../components/AuthCard'
import FormMessage from '../components/FormMessage'
import SubmitButton from '../components/SubmitButton'
import TextField from '../components/TextField'
import { api, errorMessage } from '../lib/api'
import { setToken } from '../lib/auth'
import { isBlank, useFieldErrors, type FieldErrors } from '../lib/forms'

type TokenResponse = {
  token: string
  expiresIn: number
}

type Field = 'email' | 'password'

/**
 * Signing in. A correct email and password come back as a token, which is
 * stored and then sent on every later request. A wrong email and a wrong
 * password give the same message, because the backend deliberately answers
 * both with the same 401.
 */
function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const justRegistered = (location.state as { justRegistered?: boolean } | null)?.justRegistered ?? false

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { errors, setErrors, clear } = useFieldErrors<Field>()

  // Whether the button is worth pressing. Only emptiness is checked here: a
  // password that is too short still needs to be submittable, or the person
  // never sees the message explaining what is wrong with it.
  const incomplete = isBlank(email) || password === ''

  // Only checks that something was typed. Whether the email and password are
  // actually right is the backend's call, and it answers with one message for
  // both so the form cannot be used to discover which emails have accounts.
  function validate(): FieldErrors<Field> {
    const found: FieldErrors<Field> = {}

    if (isBlank(email)) {
      found.email = 'Enter your email.'
    }
    if (password === '') {
      found.password = 'Enter your password.'
    }

    return found
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    const found = validate()
    setErrors(found)
    if (Object.keys(found).length > 0) {
      return
    }

    setSubmitting(true)

    try {
      const response = await api.post<TokenResponse>('/api/auth/login', { email, password })
      setToken(response.data.token)
      // replace, not push: the back button should not return to a login page
      // that has already been used.
      navigate('/', { replace: true })
    } catch (caught) {
      setError(errorMessage(caught, 'Could not sign in.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Sign in"
      subtitle="Welcome back to RoomMind."
      footer={
        <>
          No account yet?{' '}
          <Link to="/register" className="font-medium text-slate-900 underline">
            Create one
          </Link>
        </>
      }
    >
      {justRegistered && <FormMessage tone="success">Account created. Sign in with it below.</FormMessage>}

      {/* noValidate hands the checking to validate() above. Without it the
          browser stops the submit first and shows its own grey bubble, and the
          field styling never runs. */}
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
          label="Password"
          type="password"
          autoComplete="current-password"
          value={password}
          error={errors.password}
          onChange={(value) => {
            setPassword(value)
            clear('password')
          }}
        />

        {error && <FormMessage tone="error">{error}</FormMessage>}

        <SubmitButton submitting={submitting} incomplete={incomplete} busyLabel="Signing in…">
          Sign in
        </SubmitButton>
      </form>
    </AuthCard>
  )
}

export default LoginPage
