package dev.getelements.elements.stripe.guice;

import com.google.inject.AbstractModule;
import dev.getelements.elements.stripe.dao.MongoStripeConfigDao;
import dev.getelements.elements.stripe.dao.MongoStripeEventLogDao;
import dev.getelements.elements.stripe.dao.StripeConfigDao;
import dev.getelements.elements.stripe.dao.StripeEventLogDao;
import dev.getelements.elements.stripe.service.DefaultStripeGateway;
import dev.getelements.elements.stripe.service.StripeConfigService;
import dev.getelements.elements.stripe.service.StripeConfigServiceImpl;
import dev.getelements.elements.stripe.service.StripeEventLogService;
import dev.getelements.elements.stripe.service.StripeEventLogServiceImpl;
import dev.getelements.elements.stripe.service.StripeGateway;
import dev.getelements.elements.stripe.service.StripeService;
import dev.getelements.elements.stripe.service.StripeServiceImpl;

public class StripeModule extends AbstractModule {

    @Override
    protected void configure() {

        // Explicit self-bindings required by Guice strict mode (requireExplicitBindings)
        bind(MongoStripeConfigDao.class);
        bind(MongoStripeEventLogDao.class);
        bind(StripeConfigServiceImpl.class);
        bind(StripeEventLogServiceImpl.class);
        bind(DefaultStripeGateway.class);
        bind(StripeServiceImpl.class);

        bind(StripeConfigDao.class).to(MongoStripeConfigDao.class);
        bind(StripeEventLogDao.class).to(MongoStripeEventLogDao.class);
        bind(StripeConfigService.class).to(StripeConfigServiceImpl.class);
        bind(StripeEventLogService.class).to(StripeEventLogServiceImpl.class);
        bind(StripeGateway.class).to(DefaultStripeGateway.class);
        bind(StripeService.class).to(StripeServiceImpl.class);
    }

}
