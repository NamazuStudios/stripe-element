package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.StripeConfig;

/**
 * Reads and persists the Stripe credential configuration.
 *
 * <p>Implementations return the DB-stored values when present, falling back to the
 * Element's default attributes ({@code dev.getelements.elements.stripe.api.key} and
 * {@code dev.getelements.elements.stripe.webhook.secret}) when the DB is empty.
 */
public interface StripeConfigService {

    /** Returns the current configuration with plaintext credential values. */
    StripeConfig getConfig();

    /** Persists new credential values to the database. */
    void saveConfig(StripeConfig config);

}
