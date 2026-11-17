export const MODES = ['production', 'sandbox'] as const
export type Mode = typeof MODES[number]

export const USAGE_DAYS = 7

export interface MeterSummary {
  id: string
  displayName: string | null
  eventName: string
  status: string | null
}

export function stripeDashboardUrl(mode: Mode, path: string): string {
  return mode === 'sandbox'
    ? `https://dashboard.stripe.com/test${path}`
    : `https://dashboard.stripe.com${path}`
}

// Amounts arrive in the smallest currency unit. Assumes two minor units (USD, EUR, GBP, ...);
// zero-decimal currencies like JPY would display 100x — adjust if you bill in one.
export function formatMoney(amount: number | null | undefined, currency: string | null | undefined): string {
  if (amount == null) return '—'
  try {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency ?? 'USD' })
      .format(amount / 100)
  } catch {
    return `${amount / 100} ${currency ?? ''}`
  }
}

/** ISO-8601 start/end params for the trailing usage window (minute-aligned). */
export function usageRangeParams(): string {
  const end = new Date()
  const start = new Date(end.getTime() - USAGE_DAYS * 24 * 3600 * 1000)
  const floor = (d: Date) => d.toISOString().slice(0, 17) + '00Z'
  return `&start=${floor(start)}&end=${floor(end)}`
}

/** Sum of a page of usage aggregates. */
export function sumUsage(events: { aggregatedValue: string }[]): number {
  return events.reduce((sum, e) => sum + parseFloat(e.aggregatedValue ?? '0'), 0)
}
