package dev.getelements.elements.stripe.model;

public record StripeEventLogEntry(String stripeEventId, String eventType, String receivedAt) {}
