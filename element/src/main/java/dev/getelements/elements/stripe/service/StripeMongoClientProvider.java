package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.getelements.elements.sdk.mongo.MongoConfigurationService;
import dev.getelements.elements.sdk.mongo.MongoSslConfiguration;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.net.ssl.SSLContext;

/**
 * Creates a {@link MongoClient} scoped to the Stripe Element, using the platform's
 * {@link MongoConfigurationService} for the connection string and optional SSL settings.
 *
 * <p>The client is bundled inside the {@code .elm} (compile-scope
 * {@code mongodb-driver-sync}), so it is loaded by the element's own URLClassLoader
 * and is fully independent of the platform's Morphia/Datastore instance.
 */
@Singleton
public class StripeMongoClientProvider implements Provider<MongoClient> {

    private final MongoClient mongoClient;

    @Inject
    StripeMongoClientProvider(final MongoConfigurationService mongoConfigurationService) {

        final var config = mongoConfigurationService.getMongoConfiguration();
        final var connectionString = new ConnectionString(config.connectionString());

        final var builder = MongoClientSettings.builder()
                .applyConnectionString(connectionString);

        config.findSslConfiguration().ifPresent(ssl -> applySsl(builder, ssl));

        this.mongoClient = MongoClients.create(builder.build());
    }

    @Override
    public MongoClient get() {
        return mongoClient;
    }

    private static void applySsl(MongoClientSettings.Builder builder, MongoSslConfiguration ssl) {
        final SSLContext ctx = ssl.newSslContext();
        builder.applyToSslSettings(b -> {
            b.enabled(true);
            b.invalidHostNameAllowed(ssl.sslInvalidHostNamesAllowed());
            b.context(ctx);
        });
    }

}
