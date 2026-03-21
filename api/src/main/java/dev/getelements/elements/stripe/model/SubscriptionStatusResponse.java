package dev.getelements.elements.stripe.model;

public record SubscriptionStatusResponse(String subscriptionId, String status, String currentPeriodEnd) {}
