import { StripePlugin } from './StripePlugin'

declare const window: Window & {
  __elementsPlugins?: {
    register(route: string, component: unknown): void
  }
}

window.__elementsPlugins?.register('stripe', StripePlugin)
