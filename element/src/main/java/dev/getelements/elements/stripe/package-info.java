@ElementDefinition(recursive = true)
@GuiceElementModule(StripeModule.class)
@ElementDependency("dev.getelements.elements.sdk.dao")
@ElementDependency("dev.getelements.elements.sdk.service")
@ElementDependency("dev.getelements.elements.sdk.mongo")
@ElementPackageRequest(request = MorphiaPackageRequest.class)
package dev.getelements.elements.stripe;

import dev.getelements.elements.sdk.annotation.ElementDefinition;
import dev.getelements.elements.sdk.annotation.ElementDependency;
import dev.getelements.elements.sdk.annotation.ElementPackageRequest;
import dev.getelements.elements.sdk.dao.annotation.MorphiaPackageRequest;
import dev.getelements.elements.sdk.spi.guice.annotations.GuiceElementModule;
import dev.getelements.elements.stripe.guice.StripeModule;
