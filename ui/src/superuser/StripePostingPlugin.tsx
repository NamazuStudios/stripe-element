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

const MODE_OPTIONS = [
  { value: 'both',       label: 'Both modes' },
  { value: 'sandbox',    label: 'Sandbox' },
  { value: 'production', label: 'Production' },
]

const OUTCOME_OPTIONS = [
  { value: '',                     label: 'All outcomes' },
  { value: 'POSTED',               label: 'Posted' },
  { value: 'FAILED',               label: 'Failed' },
  { value: 'SKIPPED_EXEMPT',       label: 'Skipped (exempt)' },
  { value: 'SKIPPED_NO_CUSTOMER',  label: 'Skipped (no customer)' },
  { value: 'RECOVERED_STALE_PENDING', label: 'Recovered stale pending' },
]

const OUTCOME_STYLES: Record<string, string> = {
  POSTED:                  'bg-green-500/10 text-green-700 dark:text-green-400',
  RECOVERED_STALE_PENDING: 'bg-blue-500/10 text-blue-700 dark:text-blue-400',
  FAILED:                  'bg-destructive/10 text-destructive',
  SKIPPED_EXEMPT:          'bg-muted text-muted-foreground',
  SKIPPED_NO_CUSTOMER:     'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500',
}

export function StripePostingPlugin() {
  const limit = defaultLimit()
  const [mode, setMode] = React.useState('both')
  const [outcome, setOutcome] = React.useState('')
  const [customer, setCustomer] = React.useState('')
  const [offset, setOffset] = React.useState(0)
  const [result, setResult] = React.useState<PostingRecordResponse | null>(null)
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const [copied, setCopied] = React.useState<string | null>(null)

  async function fetchPostings(newOffset: number) {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({ limit: String(limit), offset: String(newOffset), mode })
      if (outcome) params.set('outcome', outcome)
      if (customer.trim()) params.set('customer', customer.trim())
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

  // Load on mount and whenever a filter changes.
  React.useEffect(() => { fetchPostings(0) }, [mode, outcome, customer])

  const page = Math.floor(offset / limit) + 1
  const totalPages = result ? Math.ceil(result.total / limit) : 1

  return (
    <div className="p-6 max-w-6xl">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-2xl font-bold">Meter Event Postings</h1>
        <button
          onClick={() => fetchPostings(offset)}
          disabled={loading}
          className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Refreshing\u2026' : 'Refresh'}
        </button>
      </div>
      <p className="text-sm text-muted-foreground mb-6">
        The global posting ledger: what each billing meter-event posting attempt actually did, newest first.
        Postings and failures made via recordMeterEvent are recorded automatically by this element; skips
        (exempt / no customer) are recorded by consumer elements at their reconciliation decision points.
        Click a customer ID to copy it, or open the customer in the Customers tab for its full billing view.
      </p>

      <div className="flex flex-wrap gap-3 mb-4 items-end">
        <div className="space-y-1.5">
          <label className="block text-sm font-medium">Mode</label>
          <select
            value={mode}
            onChange={e => setMode(e.target.value)}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {MODE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>
        <div className="space-y-1.5">
          <label className="block text-sm font-medium">Outcome</label>
          <select
            value={outcome}
            onChange={e => setOutcome(e.target.value)}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {OUTCOME_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>
        <div className="space-y-1.5">
          <label className="block text-sm font-medium">Customer ID</label>
          <input
            value={customer}
            onChange={e => setCustomer(e.target.value)}
            placeholder="cus_... (exact match)"
            className="w-64 rounded-md border border-border bg-background px-3 py-2 text-sm font-mono focus:outline-none focus:ring-1 focus:ring-ring"
          />
        </div>
      </div>

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4">
          {error}
        </div>
      )}

      {result && (
        result.records.length === 0
          ? <p className="text-sm text-muted-foreground">
              No posting records found. The ledger fills as meter events are posted (recorded automatically) and as
              consumer elements record their skip decisions — if nothing has posted since this feature deployed and
              the consumer wiring isn't in place yet, it will be empty.
            </p>
          : (
            <>
              <div className="rounded-md border border-border overflow-x-auto mb-4">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Time</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Outcome</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Mode</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Identifier</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Customer</th>
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
                        <td className="px-4 py-2 font-mono text-xs">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => {
                                if (!rec.customerId) return
                                navigator.clipboard.writeText(rec.customerId).then(() => {
                                  setCopied(rec.customerId!)
                                  setTimeout(() => setCopied(null), 2000)
                                })
                              }}
                              title={rec.customerId ? 'Copy customer ID' : undefined}
                              className="truncate max-w-[10rem] text-left hover:underline"
                            >
                              {rec.customerId ?? '—'}
                            </button>
                            <span className="w-14 shrink-0 text-[10px] text-green-600">
                              {copied && rec.customerId === copied ? 'copied!' : ''}
                            </span>
                          </div>
                        </td>
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
                <span className="text-muted-foreground">
                  Page {page} of {totalPages} · {result.total} total
                </span>
                <button
                  onClick={() => fetchPostings(offset + limit)}
                  disabled={loading || !result.hasMore}
                  className="rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
                >
                  Next →
                </button>
              </div>
            </>
          )
      )}
    </div>
  )
}
