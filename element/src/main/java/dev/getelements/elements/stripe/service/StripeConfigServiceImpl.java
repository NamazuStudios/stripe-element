package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.dao.StripeConfigDao;
import dev.getelements.elements.stripe.model.StripeConfigDocument;
import dev.getelements.elements.stripe.model.StripeConfig;
import jakarta.inject.Named;

@ElementServiceExport(StripeConfigService.class)
public class StripeConfigServiceImpl implements StripeConfigService {

    private final StripeConfigDao configDao;
    private final String defaultApiKey;
    private final String defaultWebhookSecret;

    @Inject
    public StripeConfigServiceImpl(
            StripeConfigDao configDao,
            @Named(StripeApplication.STRIPE_API_KEY) String defaultApiKey,
            @Named(StripeApplication.STRIPE_WEBHOOK_SECRET) String defaultWebhookSecret) {
        this.configDao = configDao;
        this.defaultApiKey = defaultApiKey;
        this.defaultWebhookSecret = defaultWebhookSecret;
    }

    @Override
    public StripeConfig getConfig() {
        return configDao.findConfig()
                .map(doc -> {
                    final var apiKey = configured(doc.getApiKey()) ? doc.getApiKey() : defaultApiKey;
                    final var secret = configured(doc.getWebhookSecret()) ? doc.getWebhookSecret() : defaultWebhookSecret;
                    return new StripeConfig(apiKey, secret);
                })
                .orElse(new StripeConfig(defaultApiKey, defaultWebhookSecret));
    }

    @Override
    public void saveConfig(StripeConfig config) {
        final var doc = new StripeConfigDocument();
        doc.setApiKey(config.apiKey());
        doc.setWebhookSecret(config.webhookSecret());
        configDao.saveConfig(doc);
    }

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }

}
