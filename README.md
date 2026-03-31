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

Set the following attributes on the Element at deploy time (via `dev.getelements.element.attributes.properties` inside the `.elm` archive or through the admin UI):

| Attribute key | Description | Default |
|---|---|---|
| `dev.getelements.elements.stripe.api.key` | Stripe secret API key (`sk_live_...` or `sk_test_...`) | *(empty)* |
| `dev.getelements.elements.stripe.webhook.secret` | Stripe webhook signing secret (`whsec_...`) | *(empty)* |
| `dev.getelements.elements.element.rs.root` | REST API root path | `/element/stripe/api` |
| `dev.getelements.elements.auth.enabled` | Enable Elements session auth filter | `true` |

Place credentials in `element/src/main/elm/dev.getelements.element.attributes.properties`:

```properties
dev.getelements.elements.stripe.api.key=sk_test_YOUR_KEY
dev.getelements.elements.stripe.webhook.secret=whsec_YOUR_SECRET
```

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
}
```

| Constant | Value | Emitted when | Arguments |
|---|---|---|---|
| `StripeEvents.PAYMENT_SUCCEEDED` | `payment_intent.succeeded` | PaymentIntent succeeded | `paymentIntentId`, `amount`, `currency` |
| `StripeEvents.PAYMENT_FAILED` | `payment_intent.payment_failed` | PaymentIntent failed | `paymentIntentId`, `failureMessage` |
| `StripeEvents.SUBSCRIPTION_CREATED` | `customer.subscription.created` | Subscription created | `subscriptionId`, `customerId`, `status` |
| `StripeEvents.SUBSCRIPTION_CANCELLED` | `customer.subscription.deleted` | Subscription cancelled | `subscriptionId`, `customerId` |

---

## Stripe Dashboard Setup

### Webhook configuration

In the Stripe Dashboard, navigate to **Developers → Webhooks → Add endpoint**.

- **Use the "Account" webhook type** (not "Event destinations / v2"). The v2 event destination format uses thin payloads and a different signing scheme that is incompatible with this Element.
- Set the endpoint URL to your deployed base URL plus the webhook path:
  ```
  https://your-host/element/stripe/api/stripe/webhook
  ```
- Subscribe to exactly these four events (others are silently ignored, but subscribing only to what you need is cleaner):
  - `payment_intent.succeeded`
  - `payment_intent.payment_failed`
  - `customer.subscription.created`
  - `customer.subscription.deleted`

After saving, Stripe shows a **Signing secret** (`whsec_...`). Copy this value into the `dev.getelements.elements.stripe.webhook.secret` attribute.

---

## Testing Webhooks Locally

Forward Stripe events to your local instance using the [Stripe CLI](https://stripe.com/docs/stripe-cli):

```bash
stripe listen --forward-to localhost:8080/element/stripe/api/stripe/webhook
```

---

## Integration Tests

Integration tests require a Stripe test-mode API key:

```bash
mvn verify -pl integration-test \
  -Dstripe.test.apiKey=sk_test_YOUR_KEY \
  -Dstripe.test.webhookSecret=whsec_YOUR_SECRET
```

Subscription tests create and tear down a real subscription automatically. Optionally supply `-Dstripe.test.priceId=price_...` to reuse an existing test-mode price instead of creating a new Product + Price each run.

Webhook signature tests (`StripeWebhookSignatureIT`) run without a network connection.

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
