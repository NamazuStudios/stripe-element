package dev.getelements.elements.stripe.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

/**
 * Morphia entity recording each verified Stripe webhook event received by this Element.
 * Stored in the {@code stripe_event_log} collection, newest-first by {@code receivedAt}.
 */
@Entity("stripe_event_log")
public class StripeEventLogDocument {

    @Id
    private ObjectId id;

    private String stripeEventId;

    private String eventType;

    private long receivedAt;

    public ObjectId getId() {
        return id;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public void setStripeEventId(String stripeEventId) {
        this.stripeEventId = stripeEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

}
