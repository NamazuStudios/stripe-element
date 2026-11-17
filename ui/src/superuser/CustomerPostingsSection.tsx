import React from 'react'
import { sessionHeaders, defaultLimit } from './api'

interface PostingRecord {
  identifier: string
  customerId: string | null
  eventName: string | null
  quantity: string | null
  outcome: string | null
  mode: string | null
  detail: string | null
  at: string
}

interface PostingRecordResponse {
  records: PostingRecord[]
  total: number
  hasMore: boolean
}

const OUTCOME_STYLES: Record<string, string> = {
  POSTED:                  'bg-green-500/10 text-green-700 dark:text-green-400',
  RECOVERED_STALE_PENDING: 'bg-blue-500/10 text-blue-700 dark:text-blue-400',
  FAILED:                  'bg-destructive/10 text-destructive',
  SKIPPED_EXEMPT:          'bg-muted text-muted-foreground',
  SKIPPED_NO_CUSTOMER:     'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500',
}

/**
 * The posting-record entries for one customer, across both modes (each row shows its mode),
 * newest first, with the identifier (idempotency key) that joins to Stripe-side usage.
 */
export function CustomerPostingsSection({ customerId }: { customerId: string }) {
  const limit = defaultLimit()
  const [offset, setOffset] = React.useState(0)
  const [result, setResult] = React.useState<PostingRecordResponse | null>(null)
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  async function fetchPostings(newOffset: number) {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({ customer: customerId, limit: String(limit), offset: String(newOffset) })
      const res = await fetch(`/element/stripe/api/stripe/postings?${params}`, {
        credentials: 'include',
        headers: sessionHeaders(),
      })
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      setResult(await res.json())
      setOffset(newOffset)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  React.useEffect(() => { fetchPostings(0) }, [customerId])

  return (
    <div>
      <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-2">Posting ledger</h2>
      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-3">
          {error}
        </div>
      )}
      {result && result.records.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No posting records for this customer. Consumer elements only record skips here — postings made via
          recordMeterEvent are recorded automatically; an empty ledger with no failed charges usually means nothing
          was posted or skipped for this customer.
        </p>
      )}
      {result && result.records.length > 0 && (
        <>
          <div className="rounded-md border border-border overflow-x-auto mb-3">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/40">
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Time</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Outcome</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Mode</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Identifier</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Event</th>
                  <th className="px-4 py-2 text-right font-medium text-muted-foreground">Qty</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Detail</th>
                </tr>
              </thead>
              <tbody>
                {result.records.map((rec, i) => (
                  <tr key={`${rec.identifier}-${rec.at}-${i}`} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
                    <td className="px-4 py-2 text-muted-foreground text-xs whitespace-nowrap">
                      {new Date(rec.at).toLocaleString()}
                    </td>
                    <td className="px-4 py-2">
                      <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${OUTCOME_STYLES[rec.outcome ?? ''] ?? 'bg-muted text-muted-foreground'}`}>
                        {rec.outcome ?? '?'}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-xs">{rec.mode?.toLowerCase() ?? '—'}</td>
                    <td className="px-4 py-2 font-mono text-xs">{rec.identifier}</td>
                    <td className="px-4 py-2 text-xs">{rec.eventName ?? '—'}</td>
                    <td className="px-4 py-2 text-xs text-right">{rec.quantity ?? '—'}</td>
                    <td className="px-4 py-2 text-xs text-muted-foreground max-w-xs truncate" title={rec.detail ?? ''}>
                      {rec.detail ?? ''}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <button
              onClick={() => fetchPostings(offset - limit)}
              disabled={loading || offset === 0}
              className="rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
            >
              ← Previous
            </button>
            <button
              onClick={() => fetchPostings(offset + limit)}
              disabled={loading || !result.hasMore}
              className="rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
            >
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  )
}
