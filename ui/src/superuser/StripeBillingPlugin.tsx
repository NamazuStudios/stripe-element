import React from 'react'
import { sessionHeaders, defaultLimit } from './api'

interface ChargeSummary {
  id: string
  paymentIntentId: string | null
  amount: number
  refundedAmount: number
  currency: string
  status: string
  description: string | null
  receiptUrl: string | null
  refunded: boolean
  created: string | null
}

interface UpcomingInvoiceLine {
  description: string | null
  amount: number
  quantity: number | null
  type: string | null
}

interface UpcomingInvoiceSummary {
  total: number
  currency: string
  periodStart: string | null
  periodEnd: string | null
  lines: UpcomingInvoiceLine[]
}

interface PaymentIntentSummary {
  id: string
  amount: number
  currency: string
  status: string
  description: string | null
  created: string | null
}

interface PaymentMethodSummary {
  id: string
  type: string
  brand: string | null
  last4: string | null
}

interface InvoiceSummary {
  id: string
  subscriptionId: string | null
  amountPaid: number
  currency: string
  status: string | null
  created: string
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

// Usage window for the summary strip and the Usage section.
const USAGE_DAYS = 7

const CHARGE_STATUS_STYLES: Record<string, string> = {
  succeeded: 'bg-green-500/10 text-green-700 dark:text-green-400',
  failed:    'bg-destructive/10 text-destructive',
  pending:   'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500',
}

const INTENT_STATUS_STYLES: Record<string, string> = {
  succeeded: 'bg-green-500/10 text-green-700 dark:text-green-400',
  canceled:  'bg-muted text-muted-foreground',
}

const SUB_STATUS_STYLES: Record<string, string> = {
  active:   'bg-green-500/10 text-green-700 dark:text-green-400',
  trialing: 'bg-blue-500/10 text-blue-700 dark:text-blue-400',
  past_due: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500',
  canceled: 'bg-muted text-muted-foreground',
}

// Amounts arrive in the smallest currency unit. Assumes two minor units (USD, EUR, GBP, ...);
// zero-decimal currencies like JPY would display 100x — adjust if you bill in one.
function formatMoney(amount: number | null | undefined, currency: string | null | undefined): string {
  if (amount == null) return '—'
  try {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency ?? 'USD' })
      .format(amount / 100)
  } catch {
    return `${amount / 100} ${currency ?? ''}`
  }
}

function Badge({ value, styles }: { value: string | null, styles: Record<string, string> }) {
  return (
    <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${styles[value ?? ''] ?? 'bg-muted text-muted-foreground'}`}>
      {value ?? '—'}
    </span>
  )
}

function SectionHeading({ children }: { children: React.ReactNode }) {
  return <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-2">{children}</h2>
}

export function StripeBillingPlugin() {
  const limit = defaultLimit()
  const [customerId, setCustomerId] = React.useState('')
  const [mode, setMode] = React.useState<Mode>('production')
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const [searched, setSearched] = React.useState(false)

  const [paymentMethods, setPaymentMethods] = React.useState<PaymentMethodSummary[] | null>(null)
  const [upcoming, setUpcoming] = React.useState<UpcomingInvoiceSummary | 'none' | null>(null)
  const [charges, setCharges] = React.useState<ChargeSummary[] | null>(null)
  const [invoices, setInvoices] = React.useState<InvoiceSummary[] | null>(null)
  const [intents, setIntents] = React.useState<PaymentIntentSummary[] | null>(null)
  const [usage, setUsage] = React.useState<Record<string, number> | null>(null)
  const [subscriptions, setSubscriptions] = React.useState<SubscriptionStatus[] | null>(null)

  const [portalLoading, setPortalLoading] = React.useState(false)
  const [portalUrl, setPortalUrl] = React.useState<string | null>(null)
  const [copied, setCopied] = React.useState(false)

  async function fetchBilling(selectedMode: Mode, id: string) {
    const headers = { ...sessionHeaders(), 'X-Stripe-Mode': selectedMode }
    const enc = encodeURIComponent(id.trim())

    async function getJson<T>(path: string): Promise<T | null> {
      const res = await fetch(path, { credentials: 'include', headers })
      return res.ok ? res.json() : null
    }

    // Usage totals per meter for the trailing window.
    async function fetchUsage(): Promise<Record<string, number> | null> {
      const meters = await getJson<MeterSummary[]>('/element/stripe/api/stripe/meters?active=true&limit=100')
      if (!meters?.length) return {}
      const end = new Date()
      const start = new Date(end.getTime() - USAGE_DAYS * 24 * 3600 * 1000)
      const range = `&start=${start.toISOString().slice(0, 17)}00Z&end=${end.toISOString().slice(0, 17)}00Z`
      const totals: Record<string, number> = {}
      await Promise.all(meters.map(async m => {
        const events = await getJson<MeterUsageSummary[]>(
          `/element/stripe/api/stripe/meters/usage?event=${encodeURIComponent(m.eventName)}&customer=${enc}${range}`)
        if (events) {
          totals[m.eventName] = events.reduce((sum, s) => sum + parseFloat(s.aggregatedValue ?? '0'), 0)
        }
      }))
      return totals
    }

    const [methods, upcomingInvoice, chargeList, invoiceList, intentList, usageTotals, subs] = await Promise.all([
      getJson<PaymentMethodSummary[]>(`/element/stripe/api/stripe/customer/${enc}/payment-methods`),
      getJson<UpcomingInvoiceSummary>(`/element/stripe/api/stripe/customer/${enc}/upcoming-invoice`), // null (204) = nothing scheduled
      getJson<ChargeSummary[]>(`/element/stripe/api/stripe/customer/${enc}/charges?limit=${Math.min(limit, 100)}`),
      getJson<InvoiceSummary[]>(`/element/stripe/api/stripe/customer/${enc}/invoices?limit=${Math.min(limit, 100)}`),
      getJson<PaymentIntentSummary[]>(`/element/stripe/api/stripe/customer/${enc}/payment-intents?limit=${Math.min(limit, 100)}`),
      fetchUsage(),
      getJson<SubscriptionListResponse>(`/element/stripe/api/stripe/customer/${enc}/subscriptions?limit=5`)
        .then(r => r?.subscriptions ?? null),
    ])

    setPaymentMethods(methods)
    setUpcoming(upcomingInvoice ?? 'none') // 204 → nothing scheduled
    setCharges(chargeList)
    setInvoices(invoiceList)
    setIntents(intentList)
    setUsage(usageTotals)
    setSubscriptions(subs)
  }

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!customerId.trim()) return
    setLoading(true)
    setError(null)
    setSearched(false)
    setPortalUrl(null)
    try {
      await fetchBilling(mode, customerId)
      setSearched(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setLoading(false)
    }
  }

  async function handleGetPortalLink() {
    setPortalLoading(true)
    setError(null)
    setPortalUrl(null)
    setCopied(false)
    try {
      const res = await fetch(`/element/stripe/api/stripe/customer/${encodeURIComponent(customerId.trim())}/portal-session`, {
        method: 'POST',
        credentials: 'include',
        headers: { ...sessionHeaders(), 'X-Stripe-Mode': mode },
      })
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      setPortalUrl(((await res.json()) as { url: string }).url)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
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

  const onFile = paymentMethods && paymentMethods.length > 0
    ? paymentMethods.filter(m => m.brand).map(m => `${m.brand!.toUpperCase()} ••${m.last4 ?? ''}`).join(', ')
    : null

  return (
    <div className="p-6 max-w-5xl">
      <h1 className="text-2xl font-bold mb-1">Stripe Billing</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Full billing posture for a customer: charges and payment attempts (one-time purchases),
        pending metered usage and the upcoming invoice, invoices, and subscriptions.
      </p>

      <form onSubmit={handleSearch} className="flex flex-wrap gap-3 mb-4 items-end">
        <div className="space-y-1.5 flex-1 min-w-48">
          <label htmlFor="billing-customer-id" className="block text-sm font-medium">Customer ID</label>
          <input
            id="billing-customer-id"
            type="text"
            value={customerId}
            onChange={e => setCustomerId(e.target.value)}
            placeholder="cus_..."
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          />
        </div>
        <div className="space-y-1.5">
          <label htmlFor="billing-mode" className="block text-sm font-medium">Mode</label>
          <select
            id="billing-mode"
            value={mode}
            onChange={e => setMode(e.target.value as Mode)}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {MODES.map(m => <option key={m} value={m}>{m === 'production' ? 'Production (live)' : 'Sandbox (test)'}</option>)}
          </select>
        </div>
        <button
          type="submit"
          disabled={loading || !customerId.trim()}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-opacity"
        >
          {loading ? 'Searching…' : 'Search'}
        </button>
        <button
          type="button"
          onClick={handleGetPortalLink}
          disabled={portalLoading || !customerId.trim()}
          className="rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-muted/40 disabled:opacity-50 transition-colors"
        >
          {portalLoading ? 'Getting link…' : 'Get Portal Link'}
        </button>
      </form>

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4">
          {error}
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
        </div>
      )}

      {searched && customerId.trim() && (
        <>
          {/* Summary strip */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
            <div className="rounded-md border border-border p-3">
              <p className="text-xs text-muted-foreground mb-1">Payment method</p>
              <p className="text-sm font-medium">
                {paymentMethods === null ? '—' : onFile ?? 'None on file'}
              </p>
            </div>
            <div className="rounded-md border border-border p-3">
              <p className="text-xs text-muted-foreground mb-1">Upcoming invoice</p>
              <p className="text-sm font-medium">
                {upcoming === 'none'
                  ? 'Nothing scheduled'
                  : upcoming
                    ? `${formatMoney(upcoming.total, upcoming.currency)} · due ${upcoming.periodEnd ? new Date(upcoming.periodEnd).toLocaleDateString() : '—'}`
                    : '—'}
              </p>
              {upcoming && upcoming !== 'none' && upcoming.lines.length > 0 && (
                <p className="text-xs text-muted-foreground mt-1 truncate" title={upcoming.lines.map(l => l.description ?? '').join('; ')}>
                  {upcoming.lines.length} line item{upcoming.lines.length === 1 ? '' : 's'}
                </p>
              )}
            </div>
            <div className="rounded-md border border-border p-3">
              <p className="text-xs text-muted-foreground mb-1">Metered usage, last {USAGE_DAYS} days</p>
              {usage === null ? (
                <p className="text-sm font-medium">—</p>
              ) : Object.keys(usage).length === 0 ? (
                <p className="text-sm font-medium">No active meters</p>
              ) : (
                Object.entries(usage).map(([event, total]) => (
                  <p key={event} className="text-sm font-medium">
                    {Number.isInteger(total) ? total : total.toFixed(4)} <span className="text-xs text-muted-foreground font-normal">{event}</span>
                  </p>
                ))
              )}
            </div>
          </div>

          {/* Charges */}
          <div className="mb-6">
            <SectionHeading>Charges (one-time purchases)</SectionHeading>
            {charges === null ? (
              <p className="text-sm text-muted-foreground">Failed to load.</p>
            ) : charges.length === 0 ? (
              <p className="text-sm text-muted-foreground">No charges.</p>
            ) : (
              <div className="rounded-md border border-border overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Date</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Amount</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Status</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Description</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Charge</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Receipt</th>
                    </tr>
                  </thead>
                  <tbody>
                    {charges.map((ch, i) => (
                      <tr key={ch.id} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
                        <td className="px-4 py-2 text-muted-foreground text-xs whitespace-nowrap">
                          {ch.created ? new Date(ch.created).toLocaleDateString() : '—'}
                        </td>
                        <td className="px-4 py-2 whitespace-nowrap">
                          {formatMoney(ch.amount, ch.currency)}
                          {ch.refundedAmount > 0 && !ch.refunded && (
                            <span className="text-xs text-orange-600 ml-1">(−{formatMoney(ch.refundedAmount, ch.currency)})</span>
                          )}
                        </td>
                        <td className="px-4 py-2">
                          <Badge value={ch.refunded ? 'refunded' : ch.status} styles={{ ...CHARGE_STATUS_STYLES, refunded: 'bg-orange-500/10 text-orange-600' }} />
                        </td>
                        <td className="px-4 py-2 text-xs max-w-xs truncate">{ch.description ?? '—'}</td>
                        <td className="px-4 py-2 font-mono text-xs">{ch.id}</td>
                        <td className="px-4 py-2 text-xs">
                          {ch.receiptUrl
                            ? <a href={ch.receiptUrl} target="_blank" rel="noreferrer" className="text-primary hover:underline">Receipt ↗</a>
                            : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Invoices */}
          <div className="mb-6">
            <SectionHeading>Invoices</SectionHeading>
            {invoices === null ? (
              <p className="text-sm text-muted-foreground">Failed to load.</p>
            ) : invoices.length === 0 ? (
              <p className="text-sm text-muted-foreground">No invoices.</p>
            ) : (
              <div className="rounded-md border border-border overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Date</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Amount paid</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Status</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Invoice</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invoices.map((inv, i) => (
                      <tr key={inv.id} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
                        <td className="px-4 py-2 text-muted-foreground text-xs whitespace-nowrap">{new Date(inv.created).toLocaleDateString()}</td>
                        <td className="px-4 py-2 whitespace-nowrap">{formatMoney(inv.amountPaid, inv.currency)}</td>
                        <td className="px-4 py-2"><Badge value={inv.status} styles={{}} /></td>
                        <td className="px-4 py-2 font-mono text-xs">{inv.id}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Payment attempts */}
          <div className="mb-6">
            <SectionHeading>Payment attempts</SectionHeading>
            {intents === null ? (
              <p className="text-sm text-muted-foreground">Failed to load.</p>
            ) : intents.length === 0 ? (
              <p className="text-sm text-muted-foreground">No PaymentIntents.</p>
            ) : (
              <div className="rounded-md border border-border overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Date</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Amount</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Status</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Intent</th>
                    </tr>
                  </thead>
                  <tbody>
                    {intents.map((pi, i) => (
                      <tr key={pi.id} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
                        <td className="px-4 py-2 text-muted-foreground text-xs whitespace-nowrap">
                          {pi.created ? new Date(pi.created).toLocaleDateString() : '—'}
                        </td>
                        <td className="px-4 py-2 whitespace-nowrap">{formatMoney(pi.amount, pi.currency)}</td>
                        <td className="px-4 py-2"><Badge value={pi.status} styles={INTENT_STATUS_STYLES} /></td>
                        <td className="px-4 py-2 font-mono text-xs">{pi.id}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Subscriptions */}
          <div>
            <SectionHeading>Subscriptions</SectionHeading>
            {subscriptions === null ? (
              <p className="text-sm text-muted-foreground">Failed to load.</p>
            ) : subscriptions.length === 0 ? (
              <p className="text-sm text-muted-foreground">No subscriptions.</p>
            ) : (
              <div className="rounded-md border border-border overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Subscription</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Status</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Renews</th>
                    </tr>
                  </thead>
                  <tbody>
                    {subscriptions.map((s, i) => (
                      <tr key={s.subscriptionId} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
                        <td className="px-4 py-2 font-mono text-xs">{s.subscriptionId}</td>
                        <td className="px-4 py-2"><Badge value={s.status} styles={SUB_STATUS_STYLES} /></td>
                        <td className="px-4 py-2 text-muted-foreground text-xs">
                          {s.currentPeriodEnd ? new Date(s.currentPeriodEnd).toLocaleDateString() : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
