import React from 'react'
import { sessionHeaders } from './api'
import { usageRangeParams, sumUsage, type Mode, type MeterSummary } from './stripeMode'

export interface ChargeSummary {
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

export interface UpcomingInvoiceLine {
  description: string | null
  amount: number
  quantity: number | null
  type: string | null
}

export interface UpcomingInvoiceSummary {
  total: number
  currency: string
  periodStart: string | null
  periodEnd: string | null
  lines: UpcomingInvoiceLine[]
}

export interface PaymentIntentSummary {
  id: string
  amount: number
  currency: string
  status: string
  description: string | null
  created: string | null
}

export interface PaymentMethodSummary {
  id: string
  type: string
  brand: string | null
  last4: string | null
}

export interface InvoiceSummary {
  id: string
  subscriptionId: string | null
  amountPaid: number
  currency: string
  status: string | null
  created: string
}

export interface SubscriptionStatus {
  subscriptionId: string
  status: string
  currentPeriodEnd: string | null
}

export interface SubscriptionListResponse {
  subscriptions: SubscriptionStatus[]
}

export interface MeterUsageSummary {
  meterId: string
  eventName: string
  aggregatedValue: string
  startTime: string | null
  endTime: string | null
}

/**
 * All billing data for one customer in one mode, fetched in parallel on mount. Each field is
 * null while loading (or when that piece failed — sections render a placeholder then; failures
 * are warned to the console). `upcoming` is the string 'none' when the customer has nothing
 * pending (the API answers 204).
 */
export function useCustomerBilling(customerId: string, mode: Mode, meters: MeterSummary[]) {
  const [loading, setLoading] = React.useState(true)
  const [paymentMethods, setPaymentMethods] = React.useState<PaymentMethodSummary[] | null>(null)
  const [upcoming, setUpcoming] = React.useState<UpcomingInvoiceSummary | 'none' | null>(null)
  const [charges, setCharges] = React.useState<ChargeSummary[] | null>(null)
  const [invoices, setInvoices] = React.useState<InvoiceSummary[] | null>(null)
  const [intents, setIntents] = React.useState<PaymentIntentSummary[] | null>(null)
  const [usage, setUsage] = React.useState<Record<string, MeterUsageSummary[] | 'error'>>({})
  const [subscriptions, setSubscriptions] = React.useState<SubscriptionStatus[] | null>(null)

  React.useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      const headers = { ...sessionHeaders(), 'X-Stripe-Mode': mode }
      const enc = encodeURIComponent(customerId)

      async function getJson<T>(path: string): Promise<T | null> {
        const res = await fetch(path, { credentials: 'include', headers })
        if (res.status === 204) return null // e.g. upcoming-invoice: nothing scheduled
        if (!res.ok) {
          console.warn(`GET ${path} failed: ${res.status} ${res.statusText}`)
          return null
        }
        return res.json()
      }

      // Hourly buckets so usage can be broken down per day.
      async function fetchUsage(): Promise<Record<string, MeterUsageSummary[] | 'error'>> {
        const result: Record<string, MeterUsageSummary[] | 'error'> = {}
        await Promise.all(meters.map(async m => {
          try {
            const res = await fetch(
              `/element/stripe/api/stripe/meters/usage?event=${encodeURIComponent(m.eventName)}&customer=${enc}${usageRangeParams()}&window=hour`,
              { credentials: 'include', headers })
            result[m.eventName] = res.ok ? await res.json() : 'error'
          } catch {
            result[m.eventName] = 'error'
          }
        }))
        return result
      }

      const [methods, upcomingInvoice, chargeList, invoiceList, intentList, usageResult, subs] = await Promise.all([
        getJson<PaymentMethodSummary[]>(`/element/stripe/api/stripe/customer/${enc}/payment-methods`),
        getJson<UpcomingInvoiceSummary>(`/element/stripe/api/stripe/customer/${enc}/upcoming-invoice`),
        getJson<ChargeSummary[]>(`/element/stripe/api/stripe/customer/${enc}/charges?limit=10`),
        getJson<InvoiceSummary[]>(`/element/stripe/api/stripe/customer/${enc}/invoices?limit=10`),
        getJson<PaymentIntentSummary[]>(`/element/stripe/api/stripe/customer/${enc}/payment-intents?limit=10`),
        fetchUsage(),
        getJson<SubscriptionListResponse>(`/element/stripe/api/stripe/customer/${enc}/subscriptions?limit=5`)
          .then(r => r?.subscriptions ?? null),
      ])

      if (cancelled) return
      setPaymentMethods(methods)
      setUpcoming(upcomingInvoice ?? 'none') // 204 → nothing scheduled
      setCharges(chargeList)
      setInvoices(invoiceList)
      setIntents(intentList)
      setUsage(usageResult)
      setSubscriptions(subs)
      setLoading(false)
    }
    load()
    return () => { cancelled = true }
  }, [customerId, mode, meters])

  // Per-meter totals keyed by local day: eventName -> dayKey -> summed usage.
  const usageByDay = React.useMemo(() => {
    const map: Record<string, Record<string, number>> = {}
    for (const [event, events] of Object.entries(usage)) {
      map[event] = {}
      if (Array.isArray(events)) {
        for (const e of events) {
          if (!e.startTime) continue
          const key = new Date(e.startTime).toDateString()
          map[event][key] = (map[event][key] ?? 0) + parseFloat(e.aggregatedValue ?? '0')
        }
      }
    }
    return map
  }, [usage])

  const usageWeekTotals = React.useMemo(() => {
    const totals: Record<string, number> = {}
    for (const [event] of Object.entries(usage)) {
      totals[event] = sumUsage(Array.isArray(usage[event]) ? usage[event] as MeterUsageSummary[] : [])
    }
    return totals
  }, [usage])

  return { loading, paymentMethods, upcoming, charges, invoices, intents, subscriptions, usage, usageByDay, usageWeekTotals }
}
