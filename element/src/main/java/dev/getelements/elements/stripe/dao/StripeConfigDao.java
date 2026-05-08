package dev.getelements.elements.stripe.dao;

import dev.getelements.elements.stripe.model.StripeConfigDocument;

import java.util.Optional;

/**
 * DAO for reading and writing the singleton Stripe configuration document.
 */
public interface StripeConfigDao {

    Optional<StripeConfigDocument> findConfig();

    void saveConfig(StripeConfigDocument document);

}
