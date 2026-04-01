import React from 'react'

interface StripeConfig {
  apiKey: string
  webhookSecret: string
}

const CONFIG_URL = '/element/stripe/api/stripe/config'

export function StripeConfigPlugin() {
  const [apiKey, setApiKey] = React.useState('')
  const [webhookSecret, setWebhookSecret] = React.useState('')
  const [loading, setLoading] = React.useState(true)
  const [saving, setSaving] = React.useState(false)
  const [saved, setSaved] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  React.useEffect(() => {
    async function loadConfig() {
      try {
        const res = await fetch(CONFIG_URL, { credentials: 'include' })
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
        const data: StripeConfig = await res.json()
        setApiKey(data.apiKey)
        setWebhookSecret(data.webhookSecret)
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e))
      } finally {
        setLoading(false)
      }
    }
    loadConfig()
  }, [])

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setSaved(false)
    setError(null)
    try {
      const res = await fetch(CONFIG_URL, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ apiKey, webhookSecret }),
      })
      if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
      setSaved(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="p-6 max-w-lg">
        <p className="text-sm text-muted-foreground">Loading configuration&hellip;</p>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-lg">
      <h1 className="text-2xl font-bold mb-1">Stripe Configuration</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Credentials are stored in the database and override the Element&rsquo;s default
        attributes. Values are masked on load.
      </p>

      <form onSubmit={handleSave} className="space-y-5">

        <div className="space-y-1.5">
          <label className="text-sm font-medium">API Key</label>
          <input
            type="password"
            value={apiKey}
            onChange={e => { setApiKey(e.target.value); setSaved(false) }}
            placeholder="sk_live_\u2026 or sk_test_\u2026"
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          />
          <p className="text-xs text-muted-foreground">
            The Stripe secret key used for PaymentIntent and Subscription API calls.
          </p>
        </div>

        <div className="space-y-1.5">
          <label className="text-sm font-medium">Webhook Signing Secret</label>
          <input
            type="password"
            value={webhookSecret}
            onChange={e => { setWebhookSecret(e.target.value); setSaved(false) }}
            placeholder="whsec_\u2026"
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          />
          <p className="text-xs text-muted-foreground">
            Found in the Stripe Dashboard under Developers &rarr; Webhooks. Must use the
            Account (not v2) webhook type.
          </p>
        </div>

        {error && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {saved && (
          <div className="rounded-md border border-green-500/50 bg-green-500/10 p-3 text-sm text-green-700 dark:text-green-400">
            Configuration saved.
          </div>
        )}

        <button
          type="submit"
          disabled={saving}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-opacity"
        >
          {saving ? 'Saving\u2026' : 'Save Configuration'}
        </button>

      </form>
    </div>
  )
}
