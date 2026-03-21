package dev.getelements.elements.stripe.guice;

import com.google.inject.PrivateModule;
import dev.getelements.elements.stripe.service.DefaultStripeGateway;
import dev.getelements.elements.stripe.service.StripeGateway;
import dev.getelements.elements.stripe.service.StripeService;
import dev.getelements.elements.stripe.service.StripeServiceImpl;

public class StripeModule extends PrivateModule {

    @Override
    protected void configure() {

        bind(StripeGateway.class).to(DefaultStripeGateway.class);
        bind(StripeService.class).to(StripeServiceImpl.class);

        expose(StripeService.class);
    }

}
