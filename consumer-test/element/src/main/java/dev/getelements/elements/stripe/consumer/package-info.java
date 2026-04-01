@ElementDefinition(recursive = true)
@GuiceElementModule(ConsumerModule.class)
@ElementDependency("dev.getelements.elements.stripe")
package dev.getelements.elements.stripe.consumer;

import dev.getelements.elements.sdk.annotation.ElementDefinition;
import dev.getelements.elements.sdk.annotation.ElementDependency;
import dev.getelements.elements.sdk.guice.GuiceElementModule;
import dev.getelements.elements.stripe.consumer.guice.ConsumerModule;
