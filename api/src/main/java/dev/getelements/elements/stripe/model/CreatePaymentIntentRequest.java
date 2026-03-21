package dev.getelements.elements.stripe.model;

public record CreatePaymentIntentRequest(long amount, String currency, String customerId) {}
