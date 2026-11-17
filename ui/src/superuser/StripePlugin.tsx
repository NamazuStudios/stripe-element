import React from 'react'
import { StripeConfigPlugin } from './StripeConfigPlugin'
import { StripeCustomersPlugin } from './StripeCustomersPlugin'
import { StripeEventLogPlugin } from './StripeEventLogPlugin'

type Tab = 'config' | 'customers' | 'events'

const TABS: { id: Tab; label: string }[] = [
  { id: 'config',    label: 'Configuration' },
  { id: 'customers', label: 'Customers' },
  { id: 'events',    label: 'Events' },
]

export function StripePlugin() {
  const [activeTab, setActiveTab] = React.useState<Tab>('config')

  return (
    <div>
      <div className="border-b border-border px-6">
        <nav className="flex gap-1" aria-label="Stripe tabs">
          {TABS.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'config'    && <StripeConfigPlugin />}
      {activeTab === 'customers' && <StripeCustomersPlugin />}
      {activeTab === 'events'    && <StripeEventLogPlugin />}
    </div>
  )
}
