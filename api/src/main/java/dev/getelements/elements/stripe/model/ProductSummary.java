package dev.getelements.elements.stripe.model;

/**
 * Lightweight view of a Stripe Product with its default price embedded.
 *
 * @param id           Stripe product ID ({@code prod_...})
 * @param name         human-readable product name
 * @param description  optional description set in the Stripe Dashboard
 * @param active       {@code false} means the product is archived
 * @param defaultPrice full summary of the product's default price, or {@code null} if unset;
 *                     callers that only need the price ID can read {@code defaultPrice.id()}
 */
public record ProductSummary(
        String id,
        String name,
        String description,
        boolean active,
        PriceSummary defaultPrice) {}
