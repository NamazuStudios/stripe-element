package dev.getelements.elements.stripe.dao;

import dev.getelements.elements.stripe.model.StripeEventLogDocument;

import java.util.List;

public interface StripeEventLogDao {

    void save(StripeEventLogDocument document);

    /**
     * Returns events ordered by {@code receivedAt} descending.
     *
     * @param type   Stripe event type filter; {@code null} returns all types
     * @param limit  max results to return
     * @param offset zero-based offset
     */
    List<StripeEventLogDocument> findEvents(String type, int limit, int offset);

    long countEvents(String type);

}
