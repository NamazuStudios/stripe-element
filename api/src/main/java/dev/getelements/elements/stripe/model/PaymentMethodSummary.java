package dev.getelements.elements.stripe.model;

/**
 * A minimal view of a Stripe PaymentMethod for callers that need to check whether
 * a customer has a card on file without exposing the full Stripe SDK model.
 *
 * @param id    Stripe payment method ID (pm_xxx)
 * @param type  Payment method type (e.g. "card")
 * @param brand Card brand (e.g. "visa"), or null for non-card types
 * @param last4 Last four digits of the card number, or null for non-card types
 */
public record PaymentMethodSummary(String id, String type, String brand, String last4) {}