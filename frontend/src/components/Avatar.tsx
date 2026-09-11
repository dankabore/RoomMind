type AvatarProps = {
  name: string
  /** Your own account: drawn dark, to match your own message bubbles. */
  self?: boolean
}

/**
 * A circle with someone's first initial, standing in for a profile picture
 * until there is one to show.
 *
 * Yours is dark and everyone else's is light, the same split the message
 * bubbles use, so "dark means you" holds everywhere on the screen.
 */
function Avatar({ name, self = false }: AvatarProps) {
  return (
    <span
      className={[
        'flex shrink-0 items-center justify-center rounded-full font-medium uppercase',
        self ? 'h-8 w-8 bg-slate-900 text-xs text-white' : 'h-9 w-9 bg-slate-200 text-sm text-slate-600',
      ].join(' ')}
    >
      {name.charAt(0)}
    </span>
  )
}

export default Avatar
