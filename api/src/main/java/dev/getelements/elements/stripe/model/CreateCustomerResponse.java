package dev.getelements.elements.stripe.model;

/**
 * Response from creating a Stripe customer.
 *
 * @param customerId Stripe customer ID ({@code cus_...})
 */
public record CreateCustomerResponse(String customerId) {}