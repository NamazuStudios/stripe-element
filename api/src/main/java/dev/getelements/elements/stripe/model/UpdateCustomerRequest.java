package dev.getelements.elements.stripe.model;

/**
 * Request body for updating a Stripe customer's contact details.
 * Fields that are {@code null} are left unchanged on the Stripe customer.
 *
 * @param email new email address, or {@code null} to leave unchanged
 * @param name  new display name, or {@code null} to leave unchanged
 */
public record UpdateCustomerRequest(String email, String name) {}
