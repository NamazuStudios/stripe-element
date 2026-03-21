package dev.getelements.elements.stripe.model;

public record CreatePaymentIntentResponse(String paymentIntentId, String clientSecret) {}
