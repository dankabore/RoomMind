import type { ReactNode } from 'react'

type AuthCardProps = {
  title: string
  subtitle: string
  children: ReactNode
  /** The line under the card, such as the link to the other auth page. */
  footer: ReactNode
}

/**
 * The white card on a grey page that both signing in and registering appear in.
 * It owns the shell only — the heading and the spacing — so the pages inside it
 * are just their form and the rules that go with it.
 */
function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center p-6">
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow">
        <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
        <p className="mt-1 text-sm text-slate-500">{subtitle}</p>

        <div className="mt-6 space-y-4">{children}</div>

        <p className="mt-6 text-sm text-slate-500">{footer}</p>
      </div>
    </div>
  )
}

export default AuthCard
