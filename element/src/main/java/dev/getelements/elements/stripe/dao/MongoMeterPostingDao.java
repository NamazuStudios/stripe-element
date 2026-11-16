package dev.getelements.elements.stripe.dao;

import com.google.inject.Inject;
import dev.getelements.elements.stripe.model.MeterPostingDocument;
import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filters;

import java.util.List;
import java.util.regex.Pattern;

public class MongoMeterPostingDao implements MeterPostingDao {

    private final Datastore datastore;

    @Inject
    public MongoMeterPostingDao(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void save(MeterPostingDocument document) {
        datastore.save(document);
    }

    @Override
    public List<MeterPostingDocument> findPostings(
            String identifierPrefix,
            String customerId,
            String eventName,
            PostingOutcome outcome,
            StripeMode mode,
            int limit,
            int offset) {

        return query(identifierPrefix, customerId, eventName, outcome, mode)
                .iterator(new FindOptions()
                        .sort(Sort.descending("createdAt"))
                        .skip(offset)
                        .limit(limit))
                .toList();
    }

    @Override
    public long countPostings(
            String identifierPrefix,
            String customerId,
            String eventName,
            PostingOutcome outcome,
            StripeMode mode) {
        return query(identifierPrefix, customerId, eventName, outcome, mode).count();
    }

    private dev.morphia.query.Query<MeterPostingDocument> query(
            String identifierPrefix,
            String customerId,
            String eventName,
            PostingOutcome outcome,
            StripeMode mode) {

        var query = datastore.find(MeterPostingDocument.class);

        if (identifierPrefix != null && !identifierPrefix.isBlank()) {
            // Anchored and quoted — a prefix match, never a free-form regex from the caller.
            query = query.filter(Filters.regex("identifier").pattern("^" + Pattern.quote(identifierPrefix)));
        }

        if (customerId != null && !customerId.isBlank()) {
            query = query.filter(Filters.eq("customerId", customerId));
        }

        if (eventName != null && !eventName.isBlank()) {
            query = query.filter(Filters.eq("eventName", eventName));
        }

        if (outcome != null) {
            query = query.filter(Filters.eq("outcome", outcome.name()));
        }

        if (mode != null) {
            query = query.filter(Filters.eq("mode", mode.name()));
        }

        return query;
    }

}
