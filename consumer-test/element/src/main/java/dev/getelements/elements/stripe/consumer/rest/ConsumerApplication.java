package dev.getelements.elements.stripe.consumer.rest;

import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ElementServiceImplementation
@ElementServiceExport(Application.class)
public class ConsumerApplication extends Application {

    /** Auth is disabled — this Element is a test harness, not a production endpoint. */
    @ElementDefaultAttribute("false")
    public static final String AUTH_ENABLED = "dev.getelements.elements.auth.enabled";

    @ElementDefaultAttribute("/element/stripe/consumer")
    public static final String RS_ROOT = "dev.getelements.elements.element.rs.root";

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(CapturedEventsEndpoint.class);
    }

}
