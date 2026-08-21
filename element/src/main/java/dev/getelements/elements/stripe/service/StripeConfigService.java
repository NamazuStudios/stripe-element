package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeMode;

/**
 * Reads and persists the Stripe credential configuration.
 *
 * <p>Implementations return the DB-stored values when present, falling back to the
 * Element's default attributes ({@code dev.getelements.elements.stripe.api.key} and
 * {@code dev.getelements.elements.stripe.webhook.secret} for {@link StripeMode#PRODUCTION},
 * {@code dev.getelements.elements.stripe.sandbox.api.key} and
 * {@code dev.getelements.elements.stripe.sandbox.webhook.secret} for {@link StripeMode#SANDBOX})
 * when the DB is empty.
 */
public interface StripeConfigService {

    /**
     * Returns the configuration for {@link #resolveDefaultMode()}. Preserved for callers that
     * predate multi-mode support.
     */
    StripeConfig getConfig();

    /** Returns the current configuration with plaintext credential values for the given mode. */
    StripeConfig getConfig(StripeMode mode);

    /**
     * Returns the raw, stored-only configuration for the given mode with no attribute-default
     * fallback applied. Used to distinguish "unset" from "set to the deployment default" when
     * diffing against masked values submitted back from a client.
     */
    StripeConfig getRawConfig(StripeMode mode);

    /**
     * Resolves which mode a request should use when none is explicitly specified: production if
     * its API key is configured, sandbox otherwise.
     */
    StripeMode resolveDefaultMode();

    /** Persists new production credential values to the database. */
    void saveConfig(StripeConfig config);

    /** Persists new credential values to the database for the given mode. */
    void saveConfig(StripeConfig config, StripeMode mode);

}
