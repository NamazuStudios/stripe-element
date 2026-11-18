package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import dev.getelements.elements.stripe.dao.StripeConfigDao;
import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeConfigDocument;
import dev.getelements.elements.stripe.model.StripeMode;

/**
 * Stripe credential configuration. The database document (managed through the Configuration
 * UI / {@code /stripe/config} endpoint) is the <strong>single source of truth</strong> — there
 * are no attribute-based fallbacks. A mode with no persisted key is unconfigured: API calls for
 * it fail with a clear error until credentials are saved.
 *
 * <p>Historical note: these credentials were once resolvable from Element attributes as a
 * fallback layer. That dual-source resolution let a displayed fallback get persisted back into
 * the database through the config round-trip (one mode's key silently overwriting the other's),
 * so the attribute layer was removed.
 */
public class StripeConfigServiceImpl implements StripeConfigService {

    private final StripeConfigDao configDao;

    @Inject
    public StripeConfigServiceImpl(StripeConfigDao configDao) {
        this.configDao = configDao;
    }

    @Override
    public StripeConfig getConfig() {
        return getConfig(resolveDefaultMode());
    }

    @Override
    public StripeConfig getConfig(StripeMode mode) {

        final var raw = getRawConfig(mode);

        if (!configured(raw.apiKey())) {
            throw new jakarta.ws.rs.InternalServerErrorException(
                    "Stripe API key not configured for mode " + mode + " — save credentials in the Configuration tab");
        }

        return raw;
    }

    @Override
    public StripeConfig getRawConfig(StripeMode mode) {
        return configDao.findConfig()
                .map(doc -> mode == StripeMode.SANDBOX
                        ? new StripeConfig(doc.getSandboxApiKey(), doc.getSandboxWebhookSecret())
                        : new StripeConfig(doc.getApiKey(), doc.getWebhookSecret()))
                .orElse(new StripeConfig(null, null));
    }

    @Override
    public StripeMode resolveDefaultMode() {
        return configured(getRawConfig(StripeMode.PRODUCTION).apiKey()) ? StripeMode.PRODUCTION : StripeMode.SANDBOX;
    }

    @Override
    public void saveConfig(StripeConfig config) {
        saveConfig(config, StripeMode.PRODUCTION);
    }

    @Override
    public void saveConfig(StripeConfig config, StripeMode mode) {
        final var doc = configDao.findConfig().orElseGet(StripeConfigDocument::new);

        if (mode == StripeMode.SANDBOX) {
            doc.setSandboxApiKey(config.apiKey());
            doc.setSandboxWebhookSecret(config.webhookSecret());
        } else {
            doc.setApiKey(config.apiKey());
            doc.setWebhookSecret(config.webhookSecret());
        }

        configDao.saveConfig(doc);
    }

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }

}
