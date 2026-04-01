import { StripeConfigPlugin } from './StripeConfigPlugin'

declare const window: Window & {
  __elementsPlugins?: {
    register(route: string, component: unknown): void
  }
}

window.__elementsPlugins?.register('stripe-config', StripeConfigPlugin)
