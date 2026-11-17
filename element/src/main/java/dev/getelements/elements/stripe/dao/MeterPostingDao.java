package dev.getelements.elements.stripe.dao;

import dev.getelements.elements.stripe.model.MeterPostingDocument;
import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.StripeMode;

import java.util.List;

/**
 * Persistence boundary for the posting-record store. Purely internal — {@code MeterPostingServiceImpl}
 * is the only caller.
 */
public interface MeterPostingDao {

    void save(MeterPostingDocument document);

    /**
     * Finds posting records, newest first, applying the given filters ({@code null} = match all).
     *
     * @param identifierPrefix matches identifiers starting with this prefix, or {@code null} for all
     */
    List<MeterPostingDocument> findPostings(
            String identifierPrefix,
            String customerId,
            String eventName,
            PostingOutcome outcome,
            StripeMode mode,
            int limit,
            int offset);

    /** Counts records matching the same filters as {@link #findPostings}. */
    long countPostings(
            String identifierPrefix,
            String customerId,
            String eventName,
            PostingOutcome outcome,
            StripeMode mode);

}
