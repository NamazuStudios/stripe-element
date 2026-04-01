package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.mongo.MongoConfigurationService;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.model.StripeConfig;
import jakarta.inject.Named;
import org.bson.Document;

/**
 * Reads Stripe credentials from a dedicated {@code stripe_config} MongoDB collection,
 * falling back to Element attribute defaults when no document is present.
 *
 * <p>The collection is accessed via the element's own bundled {@code MongoClient}
 * (created by {@link StripeMongoClientProvider}), which avoids any dependency on
 * Morphia or the platform's internal Datastore classloader.
 */
@ElementServiceExport(StripeConfigService.class)
public class StripeConfigServiceImpl implements StripeConfigService {

    private static final String COLLECTION_NAME = "stripe_config";

    private static final String DOC_ID = "stripe_config";

    private static final String KEY_API_KEY = "apiKey";

    private static final String KEY_WEBHOOK_SECRET = "webhookSecret";

    private final MongoCollection<Document> collection;

    private final String defaultApiKey;

    private final String defaultWebhookSecret;

    @Inject
    private StripeConfigServiceImpl(
            MongoClient mongoClient,
            MongoConfigurationService mongoConfigurationService,
            @Named(StripeApplication.STRIPE_API_KEY) String defaultApiKey,
            @Named(StripeApplication.STRIPE_WEBHOOK_SECRET) String defaultWebhookSecret) {

        final var connectionString = new ConnectionString(
                mongoConfigurationService.getMongoConfiguration().connectionString());
        final var dbName = connectionString.getDatabase();
        this.collection = mongoClient
                .getDatabase(dbName != null ? dbName : "elements")
                .getCollection(COLLECTION_NAME);

        this.defaultApiKey = defaultApiKey;
        this.defaultWebhookSecret = defaultWebhookSecret;
    }

    @Override
    public StripeConfig getConfig() {

        final var doc = collection.find(new Document("_id", DOC_ID)).first();

        if (doc == null) {
            return new StripeConfig(defaultApiKey, defaultWebhookSecret);
        }

        final var apiKey = doc.getString(KEY_API_KEY);
        final var secret = doc.getString(KEY_WEBHOOK_SECRET);
        final var resolvedKey = configured(apiKey) ? apiKey : defaultApiKey;
        final var resolvedSecret = configured(secret) ? secret : defaultWebhookSecret;

        return new StripeConfig(resolvedKey, resolvedSecret);
    }

    @Override
    public void saveConfig(StripeConfig config) {

        final var doc = new Document("_id", DOC_ID)
                .append(KEY_API_KEY, config.apiKey())
                .append(KEY_WEBHOOK_SECRET, config.webhookSecret());

        collection.replaceOne(
                new Document("_id", DOC_ID),
                doc,
                new ReplaceOptions().upsert(true));
    }

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }

}
