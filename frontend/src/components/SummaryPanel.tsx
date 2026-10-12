type SummaryPanelProps = {
  /** The latest summary, once one has arrived. */
  summary?: string
  loading: boolean
  /** Why the last request failed, already worded for a person to read. */
  error?: string
  /** Asks for the summary again: to retry after an error, or to catch up. */
  onRefresh: () => void
  onClose: () => void
}

/**
 * The group's summary, in a box across the top of the messages.
 *
 * It sits above the messages rather than beside them so it does not fight the
 * member panel for the side of the screen, and it has a height limit of its own
 * so a long summary scrolls inside the box instead of pushing the chat away.
 *
 * Shows one of three things: a note that the summary is being written, the
 * reason it could not be, or the summary itself.
 */
function SummaryPanel({ summary, loading, error, onRefresh, onClose }: SummaryPanelProps) {
  return (
    <section
      aria-labelledby="summary-title"
      className="border-b border-slate-200 bg-amber-50 px-6 py-4"
    >
      <div className="flex items-center justify-between gap-3">
        <h2 id="summary-title" className="text-sm font-semibold text-slate-900">
          Summary
        </h2>
        <div className="flex items-center gap-2">
          {!loading && (
            <button
              type="button"
              onClick={onRefresh}
              className="rounded-lg border border-slate-300 bg-white px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50"
            >
              {error ? 'Try again' : 'Update'}
            </button>
          )}
          <button
            type="button"
            onClick={onClose}
            aria-label="Close the summary"
            className="rounded-lg px-2 py-1 text-sm text-slate-500 hover:bg-amber-100 hover:text-slate-900"
          >
            ✕
          </button>
        </div>
      </div>

      {/* "polite" so a screen reader reads the summary, or the error, once it
          arrives, without cutting off whatever it is reading at the time. */}
      <div aria-live="polite" className="mt-2 max-h-48 overflow-y-auto">
        {loading ? (
          // The backend may wait up to a minute for the AI service, so this says
          // a wait is normal rather than looking stuck.
          <p className="text-sm text-slate-500">Summarizing the conversation… this can take a little while.</p>
        ) : error ? (
          <p role="alert" className="text-sm text-red-700">
            {error}
          </p>
        ) : (
          // pre-line keeps the line breaks the model writes, so a short list
          // stays a list.
          <p className="whitespace-pre-line text-sm leading-relaxed text-slate-800">{summary}</p>
        )}
      </div>
    </section>
  )
}

export default SummaryPanel
