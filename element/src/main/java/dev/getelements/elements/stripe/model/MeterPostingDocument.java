package dev.getelements.elements.stripe.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

/**
 * Morphia entity capturing one billing meter-event posting decision, as recorded by a consumer
 * element through {@code MeterPostingService#recordPostingOutcome}. Stored in the
 * {@code stripe_meter_event_posting} collection, newest-first by {@code createdAt}.
 */
@Entity("stripe_meter_event_posting")
public class MeterPostingDocument {

    @Id
    private ObjectId id;

    /** Idempotency key of the posting attempt, e.g. "batchId:orgId:meterName". */
    private String identifier;

    /** Stripe customer ID the event targets; null when skipped for want of a customer. */
    private String customerId;

    /** Meter event name. */
    private String eventName;

    /** Usage quantity, as a plain string to avoid any float round-tripping. */
    private String quantity;

    /** One of {@link dev.getelements.elements.stripe.model.PostingOutcome} names. */
    private String outcome;

    /** {@link dev.getelements.elements.stripe.model.StripeMode} name this attempt targeted. */
    private String mode;

    /** Human-readable skip reason or failure detail; may be null. */
    private String detail;

    /** Epoch millis of the decision. */
    private long createdAt;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
