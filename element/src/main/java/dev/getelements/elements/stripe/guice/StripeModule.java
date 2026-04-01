package dev.getelements.elements.stripe.guice;

import com.google.inject.AbstractModule;
import com.mongodb.client.MongoClient;
import dev.getelements.elements.stripe.service.DefaultStripeGateway;
import dev.getelements.elements.stripe.service.StripeConfigService;
import dev.getelements.elements.stripe.service.StripeConfigServiceImpl;
import dev.getelements.elements.stripe.service.StripeGateway;
import dev.getelements.elements.stripe.service.StripeMongoClientProvider;
import dev.getelements.elements.stripe.service.StripeService;
import dev.getelements.elements.stripe.service.StripeServiceImpl;

public class StripeModule extends AbstractModule {

    @Override
    protected void configure() {

        // Explicit self-bindings required by Guice strict mode (requireExplicitBindings)
        bind(StripeMongoClientProvider.class);
        bind(StripeConfigServiceImpl.class);
        bind(DefaultStripeGateway.class);
        bind(StripeServiceImpl.class);

        bind(MongoClient.class).toProvider(StripeMongoClientProvider.class);
        bind(StripeConfigService.class).to(StripeConfigServiceImpl.class);
        bind(StripeGateway.class).to(DefaultStripeGateway.class);
        bind(StripeService.class).to(StripeServiceImpl.class);
    }

}
