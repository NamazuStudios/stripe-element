import React from 'react'
import { sessionHeaders, defaultLimit } from './api'

interface SubscriptionStatus {
  subscriptionId: string
  status: string
  currentPeriodEnd: string | null
}

interface SubscriptionListResponse {
  subscriptions: SubscriptionStatus[]
  hasMore: boolean
  nextCursor: string | null
}

interface PortalSessionResponse {
  url: string
}

const STATUS_OPTIONS = [
  { value: '',         label: 'All (non-canceled)' },
  { value: 'all',      label: 'All (including canceled)' },
  { value: 'active',   label: 'Active' },
  { value: 'trialing', label: 'Trialing' },
  { value: 'past_due', label: 'Past due' },
  { value: 'unpaid',   label: 'Unpaid' },
  { value: 'paused',   label: 'Paused' },
  { value: 'canceled', label: 'Canceled' },
]

const STATUS_STYLES: Record<string, string> = {
  active:             'bg-green-500/10 text-green-700 dark:text-green-400',
  trialing:           'bg-blue-500/10 text-blue-700 dark:text-blue-400',
  past_due:           'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500',
  unpaid:             'bg-orange-500/10 text-orange-700 dark:text-orange-400',
  canceled:           'bg-muted text-muted-foreground',
  incomplete:         'bg-muted text-muted-foreground',
  incomplete_expired: 'bg-muted text-muted-foreground',
  paused:             'bg-muted text-muted-foreground',
}

export function StripeBillingPlugin() {
  const [customerId, setCustomerId] = React.useState('')
  const [status, setStatus] = React.useState('')
  const [limit, setLimit] = React.useState(defaultLimit())
  const [rows, setRows] = React.useState<SubscriptionStatus[] | null>(null)
  const [hasMore, setHasMore] = React.useState(false)
  const [nextCursor, setNextCursor] = React.useState<string | null>(null)
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  const [portalLoading, setPortalLoading] = React.useState(false)
  const [portalUrl, setPortalUrl] = React.useState<string | null>(null)
  const [portalError, setPortalError] = React.useState<string | null>(null)
  const [copied, setCopied] = React.useState(false)

  async function fetchPage(startingAfter: string | null, append: boolean) {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({ limit: String(limit) })
      if (status) params.set('status', status)
      if (startingAfter) params.set('startingAfter', startingAfter)
      const url = `/element/stripe/api/stripe/customer/${encodeURIComponent(customerId.trim())}/subscriptions?${params}`
      const res = await fetch(url, { credentials: 'include', headers: sessionHeaders() })
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      const data: SubscriptionListResponse = await res.json()
      setRows(append ? prev => [...(prev ?? []), ...data.subscriptions] : data.subscriptions)
      setHasMore(data.hasMore)
      setNextCursor(data.nextCursor)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!customerId.trim()) return
    setPortalUrl(null)
    fetchPage(null, false)
  }

  async function handleGetPortalLink() {
    setPortalLoading(true)
    setPortalError(null)
    setPortalUrl(null)
    setCopied(false)
    try {
      const url = `/element/stripe/api/stripe/customer/${encodeURIComponent(customerId.trim())}/portal-session`
      const res = await fetch(url, { method: 'POST', credentials: 'include', headers: sessionHeaders() })
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      const data: PortalSessionResponse = await res.json()
      setPortalUrl(data.url)
    } catch (e) {
      setPortalError(e instanceof Error ? e.message : String(e))
    } finally {
      setPortalLoading(false)
    }
  }

  function handleCopy() {
    if (!portalUrl) return
    navigator.clipboard.writeText(portalUrl).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  const limitOptions = Array.from(new Set([10, 25, 50, defaultLimit()])).sort((a, b) => a - b)

  return (
    <div className="p-6 max-w-3xl">
      <h1 className="text-2xl font-bold mb-1">Stripe Billing</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Look up subscriptions and manage billing for a Stripe customer ID.
      </p>

      <form onSubmit={handleSearch} className="flex flex-wrap gap-3 mb-4 items-end">
        <div className="space-y-1.5 flex-1 min-w-48">
          <label className="text-sm font-medium">Customer ID</label>
          <input
            type="text"
            value={customerId}
            onChange={e => setCustomerId(e.target.value)}
            placeholder="cus_\u2026"
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          />
        </div>

        <div className="space-y-1.5">
          <label className="text-sm font-medium">Status</label>
          <select
            value={status}
            onChange={e => setStatus(e.target.value)}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>

        <div className="space-y-1.5">
          <label className="text-sm font-medium">Per page</label>
          <select
            value={limit}
            onChange={e => setLimit(Number(e.target.value))}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {limitOptions.map(n => <option key={n} value={n}>{n}</option>)}
          </select>
        </div>

        <button
          type="submit"
          disabled={loading || !customerId.trim()}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-opacity"
        >
          {loading && rows === null ? 'Searching\u2026' : 'Search'}
        </button>

        <button
          type="button"
          onClick={handleGetPortalLink}
          disabled={portalLoading || !customerId.trim()}
          className="rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-muted/40 disabled:opacity-50 transition-colors"
        >
          {portalLoading ? 'Getting link\u2026' : 'Get Portal Link'}
        </button>
      </form>

      {portalError && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4">
          {portalError}
        </div>
      )}

      {portalUrl && (
        <div className="rounded-md border border-border bg-muted/20 p-3 mb-4 flex items-center gap-3">
          <span className="text-xs font-mono truncate flex-1 text-muted-foreground">{portalUrl}</span>
          <button
            type="button"
            onClick={handleCopy}
            className="shrink-0 rounded px-2 py-1 text-xs border border-border hover:bg-muted/40 transition-colors"
          >
            {copied ? 'Copied!' : 'Copy'}
          </button>
          <a
            href={portalUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="shrink-0 rounded px-2 py-1 text-xs border border-border hover:bg-muted/40 transition-colors"
          >
            Open ↗
          </a>
        </div>
      )}

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4">
          {error}
        </div>
      )}

      {rows !== null && (
        rows.length === 0
          ? <p className="text-sm text-muted-foreground">No subscriptions found.</p>
          : (
            <>
              <div className="rounded-md border border-border overflow-hidden mb-4">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Subscription ID</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Status</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Period End</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row, i) => (
                      <tr key={row.subscriptionId} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
                        <td className="px-4 py-2 font-mono">{row.subscriptionId}</td>
                        <td className="px-4 py-2">
                          <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[row.status] ?? 'bg-muted text-muted-foreground'}`}>
                            {row.status}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-muted-foreground">
                          {row.currentPeriodEnd ? new Date(row.currentPeriodEnd).toLocaleDateString() : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {hasMore && (
                <button
                  onClick={() => fetchPage(nextCursor, true)}
                  disabled={loading}
                  className="rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-muted/40 disabled:opacity-50 transition-colors"
                >
                  {loading ? 'Loading\u2026' : 'Load more'}
                </button>
              )}
            </>
          )
      )}
    </div>
  )
}
