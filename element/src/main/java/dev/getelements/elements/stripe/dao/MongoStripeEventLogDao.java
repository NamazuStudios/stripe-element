package dev.getelements.elements.stripe.dao;

import com.google.inject.Inject;
import dev.getelements.elements.stripe.model.StripeEventLogDocument;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filters;

import java.util.List;

public class MongoStripeEventLogDao implements StripeEventLogDao {

    private final Datastore datastore;

    @Inject
    public MongoStripeEventLogDao(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void save(StripeEventLogDocument document) {
        datastore.save(document);
    }

    @Override
    public List<StripeEventLogDocument> findEvents(String type, int limit, int offset) {

        var query = datastore.find(StripeEventLogDocument.class);

        if (type != null && !type.isBlank()) {
            query = query.filter(Filters.eq("eventType", type));
        }

        return query.iterator(new FindOptions()
                        .sort(Sort.descending("receivedAt"))
                        .skip(offset)
                        .limit(limit))
                .toList();
    }

    @Override
    public long countEvents(String type) {

        var query = datastore.find(StripeEventLogDocument.class);

        if (type != null && !type.isBlank()) {
            query = query.filter(Filters.eq("eventType", type));
        }

        return query.count();
    }

}
