import { useQuery } from '@tanstack/react-query'
import { api } from './lib/api'

type Health = {
  service: string
  status: string
  database: string
}

function App() {
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['health'],
    queryFn: async () => {
      const response = await api.get<Health>('/api/health')
      return response.data
    },
  })

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center p-6">
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow">
        <h1 className="text-2xl font-semibold text-slate-900">RoomMind</h1>
        <p className="mt-1 text-sm text-slate-500">Phase 1 — the project runs locally</p>

        <div className="mt-6 space-y-2 text-sm">
          {isPending && <p className="text-slate-500">Checking the backend…</p>}

          {isError && (
            <p className="text-red-600">
              Backend unreachable: {error instanceof Error ? error.message : 'unknown error'}
            </p>
          )}

          {data && (
            <>
              <Row label="Backend" value={data.status} ok={data.status === 'ok'} />
              <Row label="Database" value={data.database} ok={data.database === 'up'} />
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function Row({ label, value, ok }: { label: string; value: string; ok: boolean }) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-3">
      <span className="text-slate-600">{label}</span>
      <span className={ok ? 'font-medium text-emerald-600' : 'font-medium text-red-600'}>{value}</span>
    </div>
  )
}

export default App
