package dev.getelements.elements.stripe;

import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import dev.getelements.elements.stripe.rest.StripeConfigEndpoint;
import dev.getelements.elements.stripe.rest.StripePaymentEndpoint;
import dev.getelements.elements.stripe.rest.StripeWebhookEndpoint;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ElementServiceImplementation
@ElementServiceExport(Application.class)
public class StripeApplication extends Application {

    @ElementDefaultAttribute("true")
    public static final String AUTH_ENABLED = "dev.getelements.elements.auth.enabled";

    @ElementDefaultAttribute("/element/stripe/api")
    public static final String RS_ROOT = "dev.getelements.elements.element.rs.root";

    @ElementDefaultAttribute("")
    public static final String STRIPE_API_KEY = "dev.getelements.elements.stripe.api.key";

    @ElementDefaultAttribute("")
    public static final String STRIPE_WEBHOOK_SECRET = "dev.getelements.elements.stripe.webhook.secret";

    public static final String OPENAPI_TAG = "Stripe";

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                StripeWebhookEndpoint.class,
                StripePaymentEndpoint.class,
                StripeConfigEndpoint.class,
                StripeOpenAPIConfig.class
        );
    }

}
