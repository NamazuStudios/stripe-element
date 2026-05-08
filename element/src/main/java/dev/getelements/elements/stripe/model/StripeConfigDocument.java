package dev.getelements.elements.stripe.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

/**
 * Morphia entity representing the singleton Stripe configuration document.
 * One document per deployment, keyed by the fixed ID {@link #DOC_ID}.
 */
@Entity("stripe_config")
public class StripeConfigDocument {

    public static final String DOC_ID = "stripe_config";

    @Id
    private String id = DOC_ID;

    private String apiKey;

    private String webhookSecret;

    public String getId() {
        return id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

}
