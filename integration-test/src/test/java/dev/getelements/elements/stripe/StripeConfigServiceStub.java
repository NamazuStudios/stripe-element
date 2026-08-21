package dev.getelements.elements.stripe;

import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.service.StripeConfigService;

/**
 * No-op {@link StripeConfigService} for integration tests. {@link LiveStripeGateway} ignores the
 * mode it's given and relies on the test's global {@code Stripe.apiKey} instead, so this only
 * needs to satisfy {@link dev.getelements.elements.stripe.service.StripeServiceImpl}'s
 * {@link #resolveDefaultMode()} calls (used to build cache keys) with a stable value.
 */
class StripeConfigServiceStub implements StripeConfigService {

    @Override
    public StripeConfig getConfig() {
        return new StripeConfig("", "");
    }

    @Override
    public StripeConfig getConfig(StripeMode mode) {
        return getConfig();
    }

    @Override
    public StripeConfig getRawConfig(StripeMode mode) {
        return getConfig();
    }

    @Override
    public StripeMode resolveDefaultMode() {
        return StripeMode.PRODUCTION;
    }

    @Override
    public void saveConfig(StripeConfig config) {
    }

    @Override
    public void saveConfig(StripeConfig config, StripeMode mode) {
    }

}
