import React from 'react'
import { sessionHeaders, defaultLimit } from './api'

interface CustomerSummary {
  id: string
  name: string | null
  email: string | null
  metadata: Record<string, string>
  created: string | null
}

interface MeterSummary {
  id: string
  displayName: string | null
  eventName: string
  status: string | null
}

interface MeterUsageSummary {
  meterId: string
  eventName: string
  aggregatedValue: string
  startTime: string | null
  endTime: string | null
}

interface SubscriptionStatus {
  subscriptionId: string
  status: string
  currentPeriodEnd: string | null
}

interface SubscriptionListResponse {
  subscriptions: SubscriptionStatus[]
}

const MODES = ['production', 'sandbox'] as const
type Mode = typeof MODES[number]

// Usage lookback for the expanded customer row.
const USAGE_DAYS = 7

function stripeDashboardUrl(mode: Mode, path: string): string {
  return mode === 'sandbox'
    ? `https://dashboard.stripe.com/test${path}`
    : `https://dashboard.stripe.com${path}`
}

function displayName(c: CustomerSummary): string {
  return c.name || c.metadata['orgName'] || c.metadata['name'] || c.email || '—'
}

export function StripeCustomersPlugin() {
  const limit = defaultLimit()
  const [mode, setMode] = React.useState<Mode>('production')
  const [customers, setCustomers] = React.useState<CustomerSummary[] | null>(null)
  const [meters, setMeters] = React.useState<MeterSummary[]>([])
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const [expanded, setExpanded] = React.useState<string | null>(null)
  const [copied, setCopied] = React.useState<string | null>(null)

  async function fetchCustomers(selectedMode: Mode) {
    setLoading(true)
    setError(null)
    setExpanded(null)
    try {
      const res = await fetch(`/element/stripe/api/stripe/customers?limit=${Math.max(limit, 100)}`, {
        credentials: 'include',
        headers: { ...sessionHeaders(), 'X-Stripe-Mode': selectedMode },
      })
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      setCustomers(await res.json())

      const metersRes = await fetch('/element/stripe/api/stripe/meters?active=true&limit=100', {
        credentials: 'include',
        headers: { ...sessionHeaders(), 'X-Stripe-Mode': selectedMode },
      })
      if (metersRes.ok) setMeters(await metersRes.json())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  React.useEffect(() => { fetchCustomers(mode) }, [mode])

  function handleCopy(id: string) {
    navigator.clipboard.writeText(id).then(() => {
      setCopied(id)
      setTimeout(() => setCopied(null), 2000)
    })
  }

  return (
    <div className="p-6 max-w-6xl">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-2xl font-bold">Customers / Billing</h1>
        <div className="flex items-center gap-3">
          <select
            value={mode}
            onChange={e => setMode(e.target.value as Mode)}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {MODES.map(m => <option key={m} value={m}>{m === 'production' ? 'Production (live)' : 'Sandbox (test)'}</option>)}
          </select>
          <button
            onClick={() => fetchCustomers(mode)}
            disabled={loading}
            className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
          >
            {loading ? 'Refreshing\u2026' : 'Refresh'}
          </button>
        </div>
      </div>
      <p className="text-sm text-muted-foreground mb-6">
        Every customer on this Stripe account in the selected mode — each one is billing-enabled there.
        Expand a row for its recent metered usage (last {USAGE_DAYS} days, per meter, from Stripe's
        aggregates) and subscriptions. Cross-check posting decisions in the Postings tab.
      </p>

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4">
          {error}
        </div>
      )}

      {customers && customers.length === 0 && (
        <p className="text-sm text-muted-foreground">No customers exist in {mode} mode yet.</p>
      )}

      {customers && customers.length > 0 && (
        <div className="rounded-md border border-border overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/40">
                <th className="px-4 py-2 text-left font-medium text-muted-foreground">Customer ID</th>
                <th className="px-4 py-2 text-left font-medium text-muted-foreground">Name</th>
                <th className="px-4 py-2 text-left font-medium text-muted-foreground">Email</th>
                <th className="px-4 py-2 text-left font-medium text-muted-foreground">Created</th>
                <th className="px-4 py-2 text-left font-medium text-muted-foreground">Stripe</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody>
              {customers.map((c, i) => {
                const isExpanded = expanded === c.id
                return (
                  <React.Fragment key={c.id}>
                    <tr className={`${i % 2 === 0 ? '' : 'bg-muted/20'} ${isExpanded ? 'bg-muted/40' : ''}`}>
                      <td className="px-4 py-2 font-mono text-xs">
                        <button
                          onClick={() => handleCopy(c.id)}
                          title="Copy customer ID"
                          className="hover:underline"
                        >
                          {c.id}{copied === c.id && <span className="ml-1 text-green-600">copied!</span>}
                        </button>
                      </td>
                      <td className="px-4 py-2">{displayName(c)}</td>
                      <td className="px-4 py-2 text-muted-foreground text-xs">{c.email ?? '—'}</td>
                      <td className="px-4 py-2 text-muted-foreground text-xs">
                        {c.created ? new Date(c.created).toLocaleDateString() : '—'}
                      </td>
                      <td className="px-4 py-2 text-xs">
                        <a
                          href={stripeDashboardUrl(mode, `/customers/${c.id}`)}
                          target="_blank"
                          rel="noreferrer"
                          className="text-primary hover:underline"
                        >
                          Dashboard ↗
                        </a>
                        <span className="mx-1 text-muted-foreground">·</span>
                        <a
                          href={stripeDashboardUrl(mode, '/billing/meters')}
                          target="_blank"
                          rel="noreferrer"
                          className="text-primary hover:underline"
                        >
                          Meters ↗
                        </a>
                      </td>
                      <td className="px-4 py-2 text-right">
                        <button
                          onClick={() => setExpanded(isExpanded ? null : c.id)}
                          className="rounded-md border border-border px-2 py-1 text-xs hover:bg-muted/40 transition-colors"
                        >
                          {isExpanded ? 'Hide' : 'Usage'}
                        </button>
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr>
                        <td colSpan={6} className="px-4 py-3 bg-muted/10">
                          <CustomerDetail customerId={c.id} mode={mode} meters={meters} />
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function CustomerDetail({ customerId, mode, meters }: {
  customerId: string
  mode: Mode
  meters: MeterSummary[]
}) {
  const [usage, setUsage] = React.useState<Record<string, MeterUsageSummary[] | 'error'>>({})
  const [subscriptions, setSubscriptions] = React.useState<SubscriptionStatus[] | null>(null)
  const [loading, setLoading] = React.useState(true)

  React.useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      const headers = { ...sessionHeaders(), 'X-Stripe-Mode': mode }
      const end = new Date()
      const start = new Date(end.getTime() - USAGE_DAYS * 24 * 3600 * 1000)
      const range = `&start=${start.toISOString().slice(0, 17)}00Z&end=${end.toISOString().slice(0, 17)}00Z`

      const usageResult: Record<string, MeterUsageSummary[] | 'error'> = {}
      await Promise.all(meters.map(async m => {
        try {
          const res = await fetch(
            `/element/stripe/api/stripe/meters/usage?event=${encodeURIComponent(m.eventName)}&customer=${encodeURIComponent(customerId)}${range}`,
            { credentials: 'include', headers })
          usageResult[m.eventName] = res.ok ? await res.json() : 'error'
        } catch {
          usageResult[m.eventName] = 'error'
        }
      }))
      if (cancelled) return
      setUsage(usageResult)

      try {
        const res = await fetch(
          `/element/stripe/api/stripe/customer/${encodeURIComponent(customerId)}/subscriptions?limit=5`,
          { credentials: 'include', headers })
        if (res.ok && !cancelled) setSubscriptions((await res.json()).subscriptions)
      } catch { /* subscription detail is best-effort */ }

      if (!cancelled) setLoading(false)
    }
    load()
    return () => { cancelled = true }
  }, [customerId, mode, meters])

  const hasUsage = Object.values(usage).some(u => Array.isArray(u) && u.length > 0)

  return (
    <div className="text-xs space-y-3">
      <div>
        <p className="font-medium mb-1 text-muted-foreground">
          Metered usage, last {USAGE_DAYS} days (Stripe aggregates)
        </p>
        {meters.length === 0 ? (
          <span className="text-muted-foreground">No active meters on this account.</span>
        ) : loading ? (
          <span className="text-muted-foreground">Loading\u2026</span>
        ) : !hasUsage ? (
          <span className="text-muted-foreground">No usage recorded for this customer in the window.</span>
        ) : (
          <table className="w-full max-w-lg">
            <tbody>
              {meters.map(m => {
                const u = usage[m.eventName]
                const total = Array.isArray(u)
                  ? u.reduce((sum, s) => sum + parseFloat(s.aggregatedValue ?? '0'), 0)
                  : null
                return (
                  <tr key={m.eventName}>
                    <td className="py-0.5 pr-4 font-mono">{m.eventName}</td>
                    <td className="py-0.5 text-right font-medium">
                      {total === null
                        ? <span className="text-destructive">error</span>
                        : Number.isInteger(total) ? total : total.toFixed(4)}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {subscriptions && subscriptions.length > 0 && (
        <div>
          <p className="font-medium mb-1 text-muted-foreground">Subscriptions</p>
          {subscriptions.map(s => (
            <p key={s.subscriptionId} className="font-mono">
              {s.subscriptionId} · <span>{s.status}</span>
              {s.currentPeriodEnd && <span className="text-muted-foreground"> · renews {new Date(s.currentPeriodEnd).toLocaleDateString()}</span>}
            </p>
          ))}
        </div>
      )}
    </div>
  )
}
