package dev.getelements.elements.stripe.consumer.guice;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import dev.getelements.elements.stripe.consumer.service.StripeEventCapture;
import dev.getelements.elements.stripe.consumer.service.StripeEventCaptureImpl;

public class ConsumerModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(StripeEventCapture.class).to(StripeEventCaptureImpl.class).in(Scopes.SINGLETON);
        expose(StripeEventCapture.class);
    }

}
