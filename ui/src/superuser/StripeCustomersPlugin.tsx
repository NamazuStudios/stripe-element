import React from 'react'
import { sessionHeaders, defaultLimit } from './api'
import { useCustomerBilling } from './customerBilling'
import { CustomerPostingsSection } from './CustomerPostingsSection'
import {
  MODES, USAGE_DAYS, stripeDashboardUrl, formatMoney, usageRangeParams, type Mode, type MeterSummary,
} from './stripeMode'

interface CustomerSummary {
  id: string
  name: string | null
  email: string | null
  metadata: Record<string, string>
  created: string | null
  livemode: boolean
}

interface CustomerListResponse {
  customers: CustomerSummary[]
  hasMore: boolean
  nextCursor: string | null
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
  const [copied, setCopied] = React.useState<string | null>(null)
  const [selected, setSelected] = React.useState<CustomerSummary | null>(null)
  const [lookupId, setLookupId] = React.useState('')

  // "With usage only" filter: one request to the element's cached rollup endpoint (the element
  // fans out to Stripe server-side with bounded concurrency). customerId -> trailing-window
  // total; null = unknown (that customer's aggregates could not be fetched).
  const [usageOnly, setUsageOnly] = React.useState(false)
  const [usageWindowDays, setUsageWindowDays] = React.useState(7)
  const [usageTotals, setUsageTotals] = React.useState<Record<string, number | null>>({})
  const [usageRollupKey, setUsageRollupKey] = React.useState(0)
  const [usageRollupError, setUsageRollupError] = React.useState(false)

  // Cursor pagination: cursorStack[i] is the cursor page i+1 was fetched with (page 1 uses
  // no cursor), so Previous can walk back and Refresh can reload the current page.
  const [cursorStack, setCursorStack] = React.useState<string[]>([])
  const [hasMore, setHasMore] = React.useState(false)

  async function fetchCustomers(selectedMode: Mode, startingAfter: string | null, nextStack: string[]) {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({ limit: String(Math.max(limit, 100)) })
      if (startingAfter) params.set('startingAfter', startingAfter)
      const res = await fetch(`/element/stripe/api/stripe/customers?${params}`, {
        credentials: 'include',
        headers: { ...sessionHeaders(), 'X-Stripe-Mode': selectedMode },
      })
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      const data: CustomerListResponse = await res.json()
      setCustomers(data.customers)
      setHasMore(data.hasMore)
      setCursorStack(nextStack)
      setUsageTotals({})

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

  const currentCursor = cursorStack.length ? cursorStack[cursorStack.length - 1] : null

  React.useEffect(() => { fetchCustomers(mode, null, []) }, [mode])

  // Resolve the "with usage only" filter from the element's cached rollup.
  React.useEffect(() => {
    if (!usageOnly) return
    let cancelled = false
    setUsageRollupError(false)
    setUsageTotals({})
    ;(async () => {
      try {
        const res = await fetch(`/element/stripe/api/stripe/customers/usage?window=${usageWindowDays}${usageRollupKey > 0 ? '&refresh=true' : ''}`, {
          credentials: 'include',
          headers: { ...sessionHeaders(), 'X-Stripe-Mode': mode },
        })
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
        const rollup: { totals: { customerId: string; total: string | null }[] } = await res.json()
        if (cancelled) return
        setUsageTotals(Object.fromEntries(rollup.totals.map(t => [t.customerId, t.total != null ? parseFloat(t.total) : null])))
      } catch (e) {
        if (!cancelled) {
          console.warn('usage rollup failed', e)
          setUsageRollupError(true)
        }
      }
    })()
    return () => { cancelled = true }
  }, [usageOnly, usageWindowDays, usageRollupKey, customers, mode])

  const filteredCustomers = usageOnly && !usageRollupError
    ? (customers ?? []).filter(c => (usageTotals[c.id] ?? 0) > 0)
    : (customers ?? [])
  const usageFilterSettled = !usageOnly || usageRollupError || Object.keys(usageTotals).length > 0

  // Mode/data sanity: a production selection returning test-mode customers means the
  // production key for this deployment is a test key.
  const livemodeMismatch = (customers ?? []).length > 0 &&
    (mode === 'production' ? customers!.some(c => !c.livemode) : customers!.some(c => c.livemode))

  function handleCopy(id: string) {
    navigator.clipboard.writeText(id).then(() => {
      setCopied(id)
      setTimeout(() => setCopied(null), 2000)
    })
  }

  if (selected) {
    return (
      <CustomerDetailView
        customerId={selected.id}
        fallbackName={displayName(selected)}
        mode={mode}
        onBack={() => setSelected(null)}
      />
    )
  }

  return (
    <div className="p-6 max-w-6xl">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-2xl font-bold">Customers</h1>
        <button
          onClick={() => {
            if (usageOnly) setUsageRollupKey(k => k + 1) // bypass the rollup cache
            fetchCustomers(mode, currentCursor, cursorStack)
          }}
          disabled={loading}
          className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>
      <p className="text-sm text-muted-foreground mb-4">
        Every customer on this Stripe account in the selected mode — each one is billing-enabled there.
        Click a row for its full billing view: metered usage, charges, invoices, payment attempts,
        subscriptions, and its posting ledger.
      </p>

      <div className="flex flex-wrap gap-3 mb-4 items-end">
        <div className="space-y-1.5">
          <label htmlFor="customers-mode" className="block text-sm font-medium">Mode</label>
          <select
            id="customers-mode"
            value={mode}
            onChange={e => setMode(e.target.value as Mode)}
            className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {MODES.map(m => <option key={m} value={m}>{m === 'production' ? 'Production (live)' : 'Sandbox (test)'}</option>)}
          </select>
        </div>
        <div className="space-y-1.5">
          <label htmlFor="customers-usage-only" className="block text-sm font-medium">Filter</label>
          <label htmlFor="customers-usage-only" className="flex items-center gap-2 h-10 px-3 rounded-md border border-border bg-background text-sm cursor-pointer">
            <input
              id="customers-usage-only"
              type="checkbox"
              checked={usageOnly}
              onChange={e => setUsageOnly(e.target.checked)}
              className="accent-primary"
            />
            {usageOnly
              ? usageFilterSettled
                ? `With usage only (${filteredCustomers.length})`
                : `With usage only — summing ${Object.keys(usageTotals).length}/${customers?.length ?? 0}…`
              : 'With usage only'}
          </label>
        </div>
        {usageOnly && (
          <div className="space-y-1.5">
            <label htmlFor="customers-usage-window" className="block text-sm font-medium">Usage window</label>
            <select
              id="customers-usage-window"
              value={usageWindowDays}
              onChange={e => setUsageWindowDays(Number(e.target.value))}
              className="rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
            >
              <option value={7}>Trailing 7 days</option>
              <option value={14}>Trailing 14 days</option>
              <option value={30}>Trailing 30 days</option>
            </select>
          </div>
        )}
        <form
          className="space-y-1.5"
          onSubmit={e => {
            e.preventDefault()
            if (lookupId.trim()) setSelected({ id: lookupId.trim(), name: null, email: null, metadata: {}, created: null, livemode: mode === 'production' })
          }}
        >
          <label htmlFor="customers-lookup" className="block text-sm font-medium">Open by ID</label>
          <input
            id="customers-lookup"
            type="text"
            value={lookupId}
            onChange={e => setLookupId(e.target.value)}
            placeholder="cus_… (any mode)"
            className="w-64 rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          />
        </form>
      </div>

      {livemodeMismatch && (
        <div className="rounded-md border border-yellow-500/50 bg-yellow-500/10 p-3 text-sm text-yellow-700 dark:text-yellow-500 mb-4">
          The customer data is <strong>{mode === 'production' ? 'test-mode' : 'live-mode'}</strong> but you are viewing the{' '}
          <strong>{mode}</strong> key — the {mode} API key configured for this deployment looks like a{' '}
          {mode === 'production' ? 'test' : 'live'} key. Check the Configuration tab.
        </div>
      )}

      {usageOnly && usageRollupError && (
        <div className="rounded-md border border-yellow-500/50 bg-yellow-500/10 p-3 text-sm text-yellow-700 dark:text-yellow-500 mb-4">
          Usage totals are unavailable right now — showing all customers. Try Refresh.
        </div>
      )}

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4">
          {error}
        </div>
      )}

      {customers && !loading && filteredCustomers.length === 0 && usageFilterSettled && (
        <p className="text-sm text-muted-foreground">
          {usageOnly ? 'No customers with metered usage in the trailing window.' : `No customers exist in ${mode} mode yet.`}
        </p>
      )}

      {filteredCustomers.length > 0 && (
        <>
          <div className="rounded-md border border-border overflow-x-auto">
            <table className="w-full text-sm table-fixed">
              <thead>
                <tr className="border-b border-border bg-muted/40">
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground w-[18rem]">Customer ID</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Name</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Email</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Created</th>
                  <th className="px-4 py-2 text-left font-medium text-muted-foreground">Stripe</th>
                  <th className="px-4 py-2" />
                </tr>
              </thead>
              <tbody>
                {filteredCustomers.map((c, i) => (
                  <tr
                    key={c.id}
                    onClick={() => setSelected(c)}
                    className={`${i % 2 === 0 ? '' : 'bg-muted/20'} cursor-pointer hover:bg-muted/40 transition-colors`}
                  >
                    <td className="px-4 py-2 font-mono text-xs" onClick={e => e.stopPropagation()}>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleCopy(c.id)}
                          title="Copy customer ID"
                          className="truncate max-w-[12rem] text-left hover:underline"
                        >
                          {c.id}
                        </button>
                        <span className="w-14 shrink-0 text-[10px] text-green-600">
                          {copied === c.id ? 'copied!' : ''}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-2 truncate">{displayName(c)}</td>
                    <td className="px-4 py-2 text-muted-foreground text-xs truncate">{c.email ?? '—'}</td>
                    <td className="px-4 py-2 text-muted-foreground text-xs whitespace-nowrap">
                      {c.created ? new Date(c.created).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 py-2 text-xs whitespace-nowrap" onClick={e => e.stopPropagation()}>
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
                    <td className="px-4 py-2 text-right text-xs text-muted-foreground">Details →</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex items-center gap-3 text-sm mt-4">
            <button
              onClick={() => {
                const prevStack = cursorStack.slice(0, -1)
                fetchCustomers(mode, prevStack.length ? prevStack[prevStack.length - 1] : null, prevStack)
              }}
              disabled={loading || cursorStack.length === 0}
              className="rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
            >
              ← Previous
            </button>
            <span className="text-muted-foreground">
              {cursorStack.length > 0 ? `Page ${cursorStack.length + 1}` : 'Page 1'}
            </span>
            <button
              onClick={customers && customers.length > 0
                ? () => fetchCustomers(mode, customers[customers.length - 1].id, [...cursorStack, customers[customers.length - 1].id])
                : undefined}
              disabled={loading || !hasMore}
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

function CustomerDetailView({ customerId, fallbackName, mode, onBack }: {
  customerId: string
  fallbackName: string
  mode: Mode
  onBack: () => void
}) {
  const [meters, setMeters] = React.useState<MeterSummary[]>([])
  const [usageDays, setUsageDays] = React.useState(USAGE_DAYS)
  const billing = useCustomerBilling(customerId, mode, meters, usageDays)

  React.useEffect(() => {
    fetch('/element/stripe/api/stripe/meters?active=true&limit=100', {
      credentials: 'include',
      headers: { ...sessionHeaders(), 'X-Stripe-Mode': mode },
    }).then(res => res.ok ? res.json() : []).then(setMeters).catch(() => setMeters([]))
  }, [mode])

  const days = React.useMemo(() => {
    const list: { key: string; label: string }[] = []
    for (let i = USAGE_DAYS - 1; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      list.push({ key: d.toDateString(), label: d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) })
    }
    return list
  }, [])

  const hasUsage = Object.values(billing.usageByDay).some(days2 => Object.values(days2).some(v => v !== 0))

  function formatUsage(n: number): string {
    return n === 0 ? '·' : Number.isInteger(n) ? String(n) : n.toFixed(4)
  }

  function handlePortalLink() {
    fetch(`/element/stripe/api/stripe/customer/${encodeURIComponent(customerId)}/portal-session`, {
      method: 'POST',
      credentials: 'include',
      headers: { ...sessionHeaders(), 'X-Stripe-Mode': mode },
    }).then(async res => {
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      window.open(((await res.json()) as { url: string }).url, '_blank')
    }).catch(() => { /* portal is best-effort */ })
  }

  const onFile = billing.paymentMethods && billing.paymentMethods.length > 0
    ? billing.paymentMethods.filter(m => m.brand).map(m => `${m.brand!.toUpperCase()} ••${m.last4 ?? ''}`).join(', ')
    : null

  const weekTotal = Object.values(billing.usageWeekTotals).reduce((s, v) => s + v, 0)

  return (
    <div className="p-6 max-w-6xl">
      <button onClick={onBack} className="text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors">
        ← All customers
      </button>

      <div className="flex items-center justify-between mb-1">
        <h1 className="text-2xl font-bold">{fallbackName}</h1>
        <button
          onClick={handlePortalLink}
          className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 transition-colors"
        >
          Portal Link
        </button>
      </div>
      <p className="text-sm font-mono text-muted-foreground mb-4">
        {customerId} · {mode === 'production' ? 'production (live)' : 'sandbox (test)'} ·{' '}
        <a href={stripeDashboardUrl(mode, `/customers/${customerId}`)} target="_blank" rel="noreferrer" className="text-primary hover:underline">
          Stripe Dashboard ↗
        </a>{' · '}
        <a href={stripeDashboardUrl(mode, '/billing/meters')} target="_blank" rel="noreferrer" className="text-primary hover:underline">
          Meters ↗
        </a>
      </p>

      {/* Summary strip */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
        <div className="rounded-md border border-border p-3">
          <p className="text-xs text-muted-foreground mb-1">Payment method</p>
          <p className="text-sm font-medium">
            {billing.loading || billing.paymentMethods === null ? '—' : onFile ?? 'None on file'}
          </p>
        </div>
        <div className="rounded-md border border-border p-3">
          <p className="text-xs text-muted-foreground mb-1">Upcoming invoice</p>
          <p className="text-sm font-medium">
            {billing.loading || billing.upcoming === null
              ? '—'
              : billing.upcoming === 'none'
                ? 'Nothing scheduled'
                : `${formatMoney(billing.upcoming.total, billing.upcoming.currency)} · due ${billing.upcoming.periodEnd ? new Date(billing.upcoming.periodEnd).toLocaleDateString() : '—'}`}
          </p>
        </div>
        <div className="rounded-md border border-border p-3">
          <p className="text-xs text-muted-foreground mb-1">Metered usage, last {usageDays} days</p>
          <p className="text-sm font-medium">{billing.loading ? '—' : formatUsage(weekTotal)}</p>
        </div>
      </div>

      {/* Usage by day */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
            Metered usage by day (Stripe aggregates; local days)
          </h2>
          <select
            value={usageDays}
            onChange={e => setUsageDays(Number(e.target.value))}
            aria-label="Usage window"
            className="rounded-md border border-border bg-background px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            <option value={7}>Trailing 7 days</option>
            <option value={14}>Trailing 14 days</option>
            <option value={30}>Trailing 30 days</option>
          </select>
        </div>
        {meters.length === 0 ? (
          <p className="text-sm text-muted-foreground">No active meters on this account.</p>
        ) : !hasUsage ? (
          <p className="text-sm text-muted-foreground">No usage recorded for this customer in the window.</p>
        ) : (
          <div className="rounded-md border border-border overflow-x-auto max-w-2xl">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/40 text-muted-foreground">
                  <th className="px-3 py-2 text-left font-medium">Day</th>
                  {meters.map(m => (
                    <th key={m.eventName} className="px-3 py-2 text-right font-mono font-medium">{m.eventName}</th>
                  ))}
                  <th className="px-3 py-2 text-right font-medium">Total</th>
                </tr>
              </thead>
              <tbody>
                {[...days].reverse().map(({ key, label }) => (
                  <tr key={key}>
                    <td className="px-3 py-1 text-muted-foreground">{label}</td>
                    {meters.map(m => {
                      const value = billing.usageByDay[m.eventName]?.[key] ?? 0
                      return (
                        <td key={m.eventName} className={`px-3 py-1 text-right font-mono ${value !== 0 ? 'font-medium text-foreground' : 'text-muted-foreground/50'}`}>
                          {billing.usage[m.eventName] === 'error' ? <span className="text-destructive">err</span> : formatUsage(value)}
                        </td>
                      )
                    })}
                    <td className="px-3 py-1 text-right font-mono text-muted-foreground">
                      {(() => {
                        const dayTotal = meters.reduce((sum, m) => sum + (billing.usageByDay[m.eventName]?.[key] ?? 0), 0)
                        return dayTotal === 0 ? '·' : formatUsage(dayTotal)
                      })()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <CustomerBillingTables billing={billing} />
      <div className="mt-6">
        <CustomerPostingsSection customerId={customerId} />
      </div>
    </div>
  )
}

function CustomerBillingTables({ billing }: {
  billing: ReturnType<typeof useCustomerBilling>
}) {
  return (
    <>
      {/* Charges */}
      <div className="mb-6">
        <SectionHeading>Charges (one-time purchases)</SectionHeading>
        <DataTable empty={billing.charges !== null && billing.charges.length === 0 ? 'No charges.' : null} failed={billing.charges === null}>
          {billing.charges?.map((ch, i) => (
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
                <Badge value={ch.refunded ? 'refunded' : ch.status} styles={{ succeeded: 'bg-green-500/10 text-green-700 dark:text-green-400', failed: 'bg-destructive/10 text-destructive', refunded: 'bg-orange-500/10 text-orange-600' }} />
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
        </DataTable>
      </div>

      {/* Invoices */}
      <div className="mb-6">
        <SectionHeading>Invoices</SectionHeading>
        <DataTable empty={billing.invoices !== null && billing.invoices.length === 0 ? 'No invoices.' : null} failed={billing.invoices === null}>
          {billing.invoices?.map((inv, i) => (
            <tr key={inv.id} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
              <td className="px-4 py-2 text-muted-foreground text-xs whitespace-nowrap">{new Date(inv.created).toLocaleDateString()}</td>
              <td className="px-4 py-2 whitespace-nowrap">{formatMoney(inv.amountPaid, inv.currency)}</td>
              <td className="px-4 py-2"><Badge value={inv.status} styles={{}} /></td>
              <td className="px-4 py-2 font-mono text-xs">{inv.id}</td>
            </tr>
          ))}
        </DataTable>
      </div>

      {/* Payment attempts */}
      <div className="mb-6">
        <SectionHeading>Payment attempts</SectionHeading>
        <DataTable empty={billing.intents !== null && billing.intents.length === 0 ? 'No PaymentIntents.' : null} failed={billing.intents === null}>
          {billing.intents?.map((pi, i) => (
            <tr key={pi.id} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
              <td className="px-4 py-2 text-muted-foreground text-xs whitespace-nowrap">
                {pi.created ? new Date(pi.created).toLocaleDateString() : '—'}
              </td>
              <td className="px-4 py-2 whitespace-nowrap">{formatMoney(pi.amount, pi.currency)}</td>
              <td className="px-4 py-2">
                <Badge value={pi.status} styles={{ succeeded: 'bg-green-500/10 text-green-700 dark:text-green-400', canceled: 'bg-muted text-muted-foreground' }} />
              </td>
              <td className="px-4 py-2 font-mono text-xs">{pi.id}</td>
            </tr>
          ))}
        </DataTable>
      </div>

      {/* Subscriptions */}
      <div>
        <SectionHeading>Subscriptions</SectionHeading>
        <DataTable empty={billing.subscriptions !== null && billing.subscriptions.length === 0 ? 'No subscriptions.' : null} failed={billing.subscriptions === null}>
          {billing.subscriptions?.map((s, i) => (
            <tr key={s.subscriptionId} className={i % 2 === 0 ? '' : 'bg-muted/20'}>
              <td className="px-4 py-2 font-mono text-xs">{s.subscriptionId}</td>
              <td className="px-4 py-2">
                <Badge value={s.status} styles={{ active: 'bg-green-500/10 text-green-700 dark:text-green-400', trialing: 'bg-blue-500/10 text-blue-700 dark:text-blue-400', past_due: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500', canceled: 'bg-muted text-muted-foreground' }} />
              </td>
              <td className="px-4 py-2 text-muted-foreground text-xs">
                {s.currentPeriodEnd ? new Date(s.currentPeriodEnd).toLocaleDateString() : '—'}
              </td>
            </tr>
          ))}
        </DataTable>
      </div>
    </>
  )
}

function SectionHeading({ children }: { children: React.ReactNode }) {
  return <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-2">{children}</h2>
}

function Badge({ value, styles }: { value: string | null, styles: Record<string, string> }) {
  return (
    <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${styles[value ?? ''] ?? 'bg-muted text-muted-foreground'}`}>
      {value ?? '—'}
    </span>
  )
}

function DataTable({ children, empty, failed, headers }: {
  children: React.ReactNode
  empty: string | null
  failed: boolean
  headers?: string[]
}) {
  if (failed) return <p className="text-sm text-muted-foreground">Failed to load.</p>
  if (empty) return <p className="text-sm text-muted-foreground">{empty}</p>
  return (
    <div className="rounded-md border border-border overflow-x-auto">
      <table className="w-full text-sm">
        {headers && (
          <thead>
            <tr className="border-b border-border bg-muted/40">
              {headers.map(h => <th key={h} className="px-4 py-2 text-left font-medium text-muted-foreground">{h}</th>)}
            </tr>
          </thead>
        )}
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}
