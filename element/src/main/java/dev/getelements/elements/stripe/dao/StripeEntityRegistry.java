package dev.getelements.elements.stripe.dao;

import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import dev.getelements.elements.sdk.dao.EntityRegistry;
import dev.getelements.elements.stripe.model.StripeConfigDocument;

import java.util.List;

/**
 * Registers Stripe-specific Morphia entity classes with the platform mapper so that
 * Morphia's discriminator lookup is populated before any query runs.
 */
@ElementServiceImplementation
@ElementServiceExport(EntityRegistry.class)
public class StripeEntityRegistry implements EntityRegistry {

    @Override
    public List<Class<?>> entityClasses() {
        return List.of(StripeConfigDocument.class);
    }

}
