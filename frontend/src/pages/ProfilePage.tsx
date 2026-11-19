import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../components/FormMessage'
import { errorMessage } from '../lib/api'
import type { Language, Profile } from '../lib/profile'
import { LANGUAGES, useProfile, useUpdateProfile } from '../lib/profile'

/**
 * Your profile, at /profile: the name you would like to be shown by and the
 * language messages are translated into for you.
 *
 * The page waits for the profile before drawing the form, so the form can start
 * from the saved values instead of starting empty and being filled in a moment
 * later.
 */
function ProfilePage() {
  const { data: profile, isPending, isError, error } = useProfile()

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto w-full max-w-md">
        <div className="flex items-baseline justify-between">
          <h1 className="text-2xl font-semibold text-slate-900">Profile</h1>
          <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-900">
            Back
          </Link>
        </div>

        <div className="mt-6 rounded-xl bg-white p-6 shadow">
          {isPending && <p className="text-sm text-slate-500">Loading…</p>}

          {isError && (
            <p className="text-sm text-red-600">{errorMessage(error, 'Could not load your profile.')}</p>
          )}

          {profile && <ProfileForm profile={profile} />}
        </div>
      </div>
    </div>
  )
}

function ProfileForm({ profile }: { profile: Profile }) {
  const updateProfile = useUpdateProfile()

  const [displayName, setDisplayName] = useState(profile.displayName ?? '')
  const [language, setLanguage] = useState<Language>(profile.language)

  // Compared with what is saved, which `profile` always is: a successful save
  // replaces it in the cache and this component receives the new one. So after
  // saving, the form counts as unchanged again without being told.
  const unchanged = displayName.trim() === (profile.displayName ?? '') && language === profile.language

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    // The backend trims too; sending the trimmed value here is what makes the
    // box and the saved value match afterwards.
    const trimmed = displayName.trim()
    setDisplayName(trimmed)
    updateProfile.mutate({ displayName: trimmed, language })
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5">
      {/* Shown, not editable: the username is the handle people already know
          you by, and the email is what you sign in with. */}
      <dl className="space-y-1 text-sm">
        <div className="flex gap-2">
          <dt className="w-20 text-slate-500">Username</dt>
          <dd className="truncate font-medium text-slate-900">{profile.username}</dd>
        </div>
        <div className="flex gap-2">
          <dt className="w-20 text-slate-500">Email</dt>
          <dd className="truncate text-slate-900">{profile.email}</dd>
        </div>
      </dl>

      {/* Not TextField: that one marks every field required for screen readers,
          and this one is optional. */}
      <label className="block">
        <span className="text-sm font-medium text-slate-700">Display name</span>
        <input
          type="text"
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          // The backend's limit. Stopping the typing here means it never has to
          // say no.
          maxLength={50}
          placeholder={profile.username}
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 outline-none focus:border-slate-900"
        />
        <span className="mt-1 block text-xs text-slate-500">
          Optional. Leave it empty to go by {profile.username}.
        </span>
      </label>

      <label className="block">
        <span className="text-sm font-medium text-slate-700">Language</span>
        <select
          value={language}
          onChange={(event) => setLanguage(event.target.value as Language)}
          className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-slate-900"
        >
          {LANGUAGES.map(({ code, label }) => (
            <option key={code} value={code}>
              {label}
            </option>
          ))}
        </select>
        <span className="mt-1 block text-xs text-slate-500">
          Messages you translate are translated into this.
        </span>
      </label>

      {updateProfile.error && (
        <FormMessage tone="error">{errorMessage(updateProfile.error, 'Could not save your profile.')}</FormMessage>
      )}

      {/* Only while nothing has been changed since: once you edit again, "Saved"
          would be describing values that are no longer on screen. */}
      {updateProfile.isSuccess && unchanged && <FormMessage tone="success">Saved.</FormMessage>}

      <button
        type="submit"
        // Nothing to send when nothing differs from what is saved.
        disabled={unchanged || updateProfile.isPending}
        className="w-full rounded-lg bg-slate-900 px-4 py-2 font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-slate-900"
      >
        {updateProfile.isPending ? 'Saving…' : 'Save'}
      </button>
    </form>
  )
}

export default ProfilePage
