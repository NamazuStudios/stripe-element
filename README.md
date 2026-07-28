# stripe - Namazu Elements Stripe Integration

A production-ready [Stripe](https://stripe.com) payment processing Element for [Namazu Elements](https://namazustudios.com) 3.7.

## Features

- **Webhook receiver** - verifies Stripe webhook signatures and publishes typed internal events
- **PaymentIntent API** - authenticated endpoint to create Stripe PaymentIntents from the front-end
- **Subscription status API** - authenticated endpoint to query subscription state
- **Typed event bus** - Stripe events are translated to strongly-typed records that other Elements can subscribe to

---

## Build & Run

```bash
# Build everything (requires Java 21 + Maven)
mvn install

# Start local MongoDB
docker compose -f services-dev/docker-compose.yml up -d

# Run locally
mvn -pl debug exec:java
```

---

## Configuration

| Attribute key | Description | Default |
|---|---|---|
| `dev.getelements.elements.stripe.api.key` | Stripe secret API key (`sk_live_...` or `sk_test_...`) | *(empty)* |
| `dev.getelements.elements.stripe.webhook.secret` | Stripe webhook signing secret (`whsec_...`) | *(empty)* |
| `dev.getelements.elements.element.rs.root` | REST API root path | `/element/stripe/api` |
| `dev.getelements.elements.auth.enabled` | Enable Elements session auth filter | `true` |

**Deployed instances** — set attributes via the admin panel under **Element Management**. Select the deployment and edit its attributes directly; no rebuild is required.

**Local development** — set attributes in `run.java` before the element loads, or pass them as system properties on the command line:

```bash
mvn -pl debug exec:java \
  -Ddev.getelements.elements.stripe.api.key=sk_test_YOUR_KEY \
  -Ddev.getelements.elements.stripe.webhook.secret=whsec_YOUR_SECRET
```

Defaults can also be baked into `element/src/main/elm/dev.getelements.element.attributes.properties` and will be packaged into the `.elm` archive, but these are overridden by anything set at the deployment level.

---

## REST Endpoints

All endpoints are mounted under the configured RS root (default `/element/stripe/api`).

| Method | Path | Auth required | Description |
|---|---|---|---|
| `POST` | `/stripe/webhook` | No | Stripe webhook receiver (signature-verified) |
| `POST` | `/stripe/payment-intent` | Yes (session_secret) | Create a Stripe PaymentIntent |
| `GET` | `/stripe/subscription/{subscriptionId}` | Yes (session_secret) | Get subscription status |

OpenAPI spec is available at `http://localhost:8080/api/rest/openapi.json` when running locally.

---

## Emitted Events

Other Elements can subscribe to Stripe events using `@ElementEventConsumer` with the name constants from `StripeEvents` (in the `api` module). Annotate a method on any Guice-managed service:

```java
import com.google.inject.Inject;
import dev.getelements.elements.sdk.annotation.ElementEventConsumer;
import dev.getelements.elements.stripe.StripeEvents;

public class EntitlementService {

    @Inject
    private UserInventoryDao userInventoryDao;

    @ElementEventConsumer(StripeEvents.PAYMENT_SUCCEEDED)
    public void onPaymentSucceeded() {
        // called whenever a payment_intent.succeeded webhook is received
    }

    @ElementEventConsumer(StripeEvents.SUBSCRIPTION_CANCELLED)
    public void onSubscriptionCancelled() {
        // revoke access, notify player, etc.
    }

    @ElementEventConsumer(StripeEvents.RAW_WEBHOOK)
    public void onAnyWebhook() {
        // called for every verified Stripe webhook — use for event types
        // that don't have a dedicated typed event
    }
}
```

### Typed events

Published for the most common webhook types. Each carries strongly-typed fields so consumers don't need to parse Stripe payloads directly.

| Constant | Stripe event | Emitted when | Fields |
|---|---|---|---|
| `PAYMENT_SUCCEEDED` | `payment_intent.succeeded` | One-time payment confirmed | `paymentIntentId`, `amount`, `currency` |
| `PAYMENT_FAILED` | `payment_intent.payment_failed` | One-time payment failed | `paymentIntentId`, `failureMessage` |
| `INVOICE_PAYMENT_SUCCEEDED` | `invoice.payment_succeeded` | Subscription renewal paid | `invoiceId`, `paymentIntentId`, `amountPaid`, `currency` |
| `INVOICE_PAYMENT_FAILED` | `invoice.payment_failed` | Subscription renewal failed | `invoiceId`, `subscriptionId`, `customerId`, `failureMessage` |
| `SUBSCRIPTION_CREATED` | `customer.subscription.created` | Subscription started | `subscriptionId`, `customerId`, `status` |
| `SUBSCRIPTION_UPDATED` | `customer.subscription.updated` | Subscription changed (plan, quantity, trial→active, etc.) | `subscriptionId`, `customerId`, `status` |
| `SUBSCRIPTION_CANCELLED` | `customer.subscription.deleted` | Subscription cancelled | `subscriptionId`, `customerId` |
| `SUBSCRIPTION_TRIAL_WILL_END` | `customer.subscription.trial_will_end` | Trial ends in 3 days | `subscriptionId`, `customerId`, `trialEnd` (ISO-8601) |

### Raw event

`StripeEvents.RAW_WEBHOOK` (`stripe.webhook`) is published for **every** verified webhook, including the ones above. Fields: `type` (Stripe event type string), `eventId`, `rawJson` (full webhook payload). Use this to handle event types not covered by the typed events above without forking the element.

---

## Multi-Environment Deployments

The recommended approach for running separate environments (e.g. `MyGame_Dev`, `MyGame_Staging`, `MyGame_Prod`) is to deploy a distinct instance of this Element for each environment.

Each deployment gets its own:
- Stripe API key and webhook secret (configured independently via the **Stripe → Configuration** tab in the superuser UI)
- MongoDB database (or at minimum a separate MongoDB instance), so configuration and event log data are naturally isolated with no additional namespacing required

In the Elements platform this maps directly to creating separate application deployments — one per environment. Because each deployment connects to its own database, the DAO layer requires no special configuration to achieve isolation.

> **Note:** Do not share a MongoDB database between multiple deployments of this Element. The configuration and event log collections use fixed names and are not namespaced per deployment.

---

## Stripe Dashboard Setup

### Webhook configuration

In the Stripe Dashboard, navigate to **Developers → Webhooks → Add endpoint**.

- **Use the "Account" webhook type** (not "Event destinations / v2"). The v2 event destination format uses thin payloads and a different signing scheme that is incompatible with this Element.
- Set the endpoint URL to your deployed base URL plus the webhook path:
  ```
  https://your-host/element/stripe/api/stripe/webhook
  ```
- Subscribe to the events you need. Any webhook that arrives with a valid signature will be forwarded as a `RAW_WEBHOOK` event regardless; the typed events below are also published for the corresponding types:
  - `payment_intent.succeeded`
  - `payment_intent.payment_failed`
  - `invoice.payment_succeeded`
  - `invoice.payment_failed`
  - `customer.subscription.created`
  - `customer.subscription.updated`
  - `customer.subscription.deleted`
  - `customer.subscription.trial_will_end`

After saving, Stripe shows a **Signing secret** (`whsec_...`). Copy this value into the `dev.getelements.elements.stripe.webhook.secret` attribute.

---

## Testing Webhooks Locally

Forward Stripe events to your local instance using the [Stripe CLI](https://stripe.com/docs/stripe-cli):

```bash
stripe listen --forward-to localhost:8080/element/stripe/api/stripe/webhook
```

---

## Integration Tests

Integration tests require a Stripe test-mode API key, sourced from environment variables so CI can inject them as secrets:

| Env var | Maven property | Purpose |
|---------|----------------|---------|
| `STRIPE_TEST_API_KEY` | `stripe.test.apiKey` | Stripe test-mode secret key |
| `STRIPE_TEST_CUSTOMER_ID` | `stripe.test.customerId` | Existing test-mode customer to reuse |
| `STRIPE_TEST_PRICE_ID` | `stripe.test.priceId` | Existing test-mode price to reuse |

```bash
export STRIPE_TEST_API_KEY=sk_test_YOUR_KEY
mvn verify -pl integration-test
```

Each variable can still be overridden per-run with the matching `-Dstripe.test.*` system property (e.g. `-Dstripe.test.apiKey=sk_test_OTHER_KEY`).

Subscription tests create and tear down a real subscription automatically. Optionally set `STRIPE_TEST_PRICE_ID` (or `-Dstripe.test.priceId=price_...`) to reuse an existing test-mode price instead of creating a new Product + Price each run.

Webhook tests (`StripeWebhookSignatureIT`, `StripeWebhookEndpointIT`) run without a network connection or Stripe credentials — they generate and verify their own HMAC signatures against a fixed, non-secret test key.

---

## Maven Coordinates

```xml
<!-- .elm archive (for deployment) -->
<dependency>
    <groupId>dev.getelements.elements.stripe</groupId>
    <artifactId>element</artifactId>
    <version>1.0-SNAPSHOT</version>
    <type>elm</type>
</dependency>

<!-- API interfaces (for other Elements that depend on this one) -->
<dependency>
    <groupId>dev.getelements.elements.stripe</groupId>
    <artifactId>api</artifactId>
    <version>1.0-SNAPSHOT</version>
    <classifier>dev.getelements.elements.stripe.api</classifier>
    <scope>provided</scope>
</dependency>
```
