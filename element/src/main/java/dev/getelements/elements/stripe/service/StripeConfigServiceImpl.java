package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.dao.StripeConfigDao;
import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeConfigDocument;
import dev.getelements.elements.stripe.model.StripeMode;
import jakarta.inject.Named;

@ElementServiceExport(StripeConfigService.class)
public class StripeConfigServiceImpl implements StripeConfigService {

    private final StripeConfigDao configDao;
    private final String defaultApiKey;
    private final String defaultWebhookSecret;
    private final String defaultSandboxApiKey;
    private final String defaultSandboxWebhookSecret;

    @Inject
    public StripeConfigServiceImpl(
            StripeConfigDao configDao,
            @Named(StripeApplication.STRIPE_API_KEY) String defaultApiKey,
            @Named(StripeApplication.STRIPE_WEBHOOK_SECRET) String defaultWebhookSecret,
            @Named(StripeApplication.STRIPE_SANDBOX_API_KEY) String defaultSandboxApiKey,
            @Named(StripeApplication.STRIPE_SANDBOX_WEBHOOK_SECRET) String defaultSandboxWebhookSecret) {
        this.configDao = configDao;
        this.defaultApiKey = defaultApiKey;
        this.defaultWebhookSecret = defaultWebhookSecret;
        this.defaultSandboxApiKey = defaultSandboxApiKey;
        this.defaultSandboxWebhookSecret = defaultSandboxWebhookSecret;
    }

    @Override
    public StripeConfig getConfig() {
        return getConfig(resolveDefaultMode());
    }

    @Override
    public StripeConfig getConfig(StripeMode mode) {
        final var raw = getRawConfig(mode);
        final var defaultApiKeyForMode = mode == StripeMode.SANDBOX ? defaultSandboxApiKey : defaultApiKey;
        final var defaultSecretForMode = mode == StripeMode.SANDBOX ? defaultSandboxWebhookSecret : defaultWebhookSecret;
        final var apiKey = configured(raw.apiKey()) ? raw.apiKey() : defaultApiKeyForMode;
        final var secret = configured(raw.webhookSecret()) ? raw.webhookSecret() : defaultSecretForMode;
        return new StripeConfig(apiKey, secret);
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
        return configured(getConfig(StripeMode.PRODUCTION).apiKey()) ? StripeMode.PRODUCTION : StripeMode.SANDBOX;
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
