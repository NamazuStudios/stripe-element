import React from 'react'
import { sessionHeaders } from './api'

interface StripeConfig {
  apiKey: string
  webhookSecret: string
}

interface StripeDualConfig {
  production: StripeConfig
  sandbox: StripeConfig
}

const CONFIG_URL = '/element/stripe/api/stripe/config'

const EMPTY_CONFIG: StripeConfig = { apiKey: '', webhookSecret: '' }

type ModeTab = 'production' | 'sandbox'

const MODE_TABS: { id: ModeTab; label: string }[] = [
  { id: 'production', label: 'Production' },
  { id: 'sandbox', label: 'Sandbox' },
]

function ConfigFields({
  description,
  apiKeyPlaceholder,
  config,
  onChange,
}: {
  description: string
  apiKeyPlaceholder: string
  config: StripeConfig
  onChange: (config: StripeConfig) => void
}) {
  return (
    <fieldset className="space-y-5">
      <p className="text-sm text-muted-foreground">{description}</p>

      <div className="space-y-1.5">
        <label className="text-sm font-medium">API Key</label>
        <input
          type="password"
          value={config.apiKey}
          onChange={e => onChange({ ...config, apiKey: e.target.value })}
          placeholder={apiKeyPlaceholder}
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
          value={config.webhookSecret}
          onChange={e => onChange({ ...config, webhookSecret: e.target.value })}
          placeholder="whsec_…"
          className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
        />
        <p className="text-xs text-muted-foreground">
          Found in the Stripe Dashboard under Developers &rarr; Webhooks. Must use the
          Account (not v2) webhook type.
        </p>
      </div>
    </fieldset>
  )
}

export function StripeConfigPlugin() {
  const [activeMode, setActiveMode] = React.useState<ModeTab>('production')
  const [production, setProduction] = React.useState<StripeConfig>(EMPTY_CONFIG)
  const [sandbox, setSandbox] = React.useState<StripeConfig>(EMPTY_CONFIG)
  const [loading, setLoading] = React.useState(true)
  const [saving, setSaving] = React.useState(false)
  const [saved, setSaved] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  React.useEffect(() => {
    async function loadConfig() {
      try {
        const res = await fetch(CONFIG_URL, { credentials: 'include', headers: sessionHeaders() })
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
        const data: StripeDualConfig = await res.json()
        setProduction(data.production)
        setSandbox(data.sandbox)
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
        headers: sessionHeaders(),
        body: JSON.stringify({ production, sandbox }),
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
        attributes. Values are masked on load. Both sets of credentials can be configured at
        once and selected per request via the header:
        <code>X-Stripe-Mode: (production/sandbox)</code>{' '}
        Requests that omit the header use production if configured, sandbox otherwise.
      </p>

      <form onSubmit={handleSave} className="space-y-6">

        <div className="border-b border-border">
          <nav className="flex gap-1" aria-label="Stripe mode tabs">
            {MODE_TABS.map(tab => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveMode(tab.id)}
                className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                  activeMode === tab.id
                    ? 'border-primary text-foreground'
                    : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {activeMode === 'production' && (
          <ConfigFields
            description="Live-mode credentials used by default."
            apiKeyPlaceholder="sk_live_…"
            config={production}
            onChange={config => { setProduction(config); setSaved(false) }}
          />
        )}

        {activeMode === 'sandbox' && (
          <ConfigFields
            description="Test-mode credentials, selected via X-Stripe-Mode: sandbox."
            apiKeyPlaceholder="sk_test_…"
            config={sandbox}
            onChange={config => { setSandbox(config); setSaved(false) }}
          />
        )}

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
          {saving ? 'Saving…' : 'Save Configuration'}
        </button>

      </form>
    </div>
  )
}
