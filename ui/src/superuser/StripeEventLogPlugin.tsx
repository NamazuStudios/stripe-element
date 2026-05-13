import React from 'react'
import { sessionHeaders, defaultLimit } from './api'

interface StripeEventLogEntry {
  stripeEventId: string
  eventType: string
  receivedAt: string
}

interface StripeEventLogResponse {
  events: StripeEventLogEntry[]
  total: number
  hasMore: boolean
}

const EVENT_TYPE_OPTIONS = [
  { value: '',                                    label: 'All types' },
  { value: 'payment_intent.succeeded',            label: 'Payment succeeded' },
  { value: 'payment_intent.payment_failed',       label: 'Payment failed' },
  { value: 'invoice.payment_succeeded',           label: 'Invoice paid' },
  { value: 'invoice.payment_failed',              label: 'Invoice payment failed' },
  { value: 'customer.subscription.created',       label: 'Subscription created' },
  { value: 'customer.subscription.updated',       label: 'Subscription updated' },
  { value: 'customer.subscription.deleted',       label: 'Subscription canceled' },
  { value: 'customer.subscription.trial_will_end', label: 'Trial ending soon' },
]

const EVENT_TYPE_STYLES: Record<string, string> = {
  'payment_intent.succeeded':             'bg-green-500/10 text-green-700 dark:text-green-400',
  'invoice.payment_succeeded':            'bg-green-500/10 text-green-700 dark:text-green-400',
  'payment_intent.payment_failed':        'bg-destructive/10 text-destructive',
  'invoice.payment_failed':               'bg-destructive/10 text-destructive',
  'customer.subscription.deleted':        'bg-muted text-muted-foreground',
  'customer.subscription.trial_will_end': 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500',
}

export function StripeEventLogPlugin() {
  const limit = defaultLimit()
  const [type, setType] = React.useState('')
  const [offset, setOffset] = React.useState(0)
  const [result, setResult] = React.useState<StripeEventLogResponse | null>(null)
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  async function fetchEvents(newOffset: number) {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({ limit: String(limit), offset: String(newOffset) })
      if (type) params.set('type', type)
      const res = await fetch(`/element/stripe/api/stripe/events?${params}`, {
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

  // Load on mount and whenever type filter changes.
  React.useEffect(() => { fetchEvents(0) }, [type])

  const page = Math.floor(offset / limit) + 1
  const totalPages = result ? Math.ceil(result.total / limit) : 1

  return (
    <div className="p-6 max-w-4xl">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-2xl font-bold">Stripe Webhook Events</h1>
        <button
          onClick={() => fetchEvents(offset)}
          disabled={loading}
          className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Refreshing\u2026' : 'Refresh'}
        </button>
      </div>
      <p className="text-sm text-muted-foreground mb-6">
        Verified webhook events received by this Element, newest first.
      </p>

      <div className="flex flex-wrap gap-3 mb-4 items-end">
        <div className="space-y-1.5">
          <label className="text-sm font-medium">Event type</label>
          <select
            value={type}
            onChange={e => setType(e.target.value)}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {EVENT_TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>
      </div>

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4">
          {error}
        </div>
      )}

      {result && (
        result.events.length === 0
          ? <p className="text-sm text-muted-foreground">No events found.</p>
          : (
            <>
              <div className="rounded-md border border-border overflow-hidden mb-4">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Event ID</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Type</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Received</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.events.map((evt, i) => (
                      <tr key={evt.stripeEventId} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
                        <td className="px-4 py-2 font-mono text-xs">{evt.stripeEventId}</td>
                        <td className="px-4 py-2">
                          <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${EVENT_TYPE_STYLES[evt.eventType] ?? 'bg-muted text-muted-foreground'}`}>
                            {evt.eventType}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-muted-foreground text-xs">
                          {new Date(evt.receivedAt).toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="flex items-center gap-3 text-sm">
                <button
                  onClick={() => fetchEvents(offset - limit)}
                  disabled={loading || offset === 0}
                  className="rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
                >
                  ← Previous
                </button>
                <span className="text-muted-foreground">
                  Page {page} of {totalPages} · {result.total} total
                </span>
                <button
                  onClick={() => fetchEvents(offset + limit)}
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
