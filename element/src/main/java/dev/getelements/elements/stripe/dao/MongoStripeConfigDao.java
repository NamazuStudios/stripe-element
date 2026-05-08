package dev.getelements.elements.stripe.dao;

import com.google.inject.Inject;
import dev.getelements.elements.stripe.model.StripeConfigDocument;
import dev.morphia.Datastore;
import dev.morphia.query.filters.Filters;

import java.util.Optional;

public class MongoStripeConfigDao implements StripeConfigDao {

    private final Datastore datastore;

    @Inject
    public MongoStripeConfigDao(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Optional<StripeConfigDocument> findConfig() {
        return Optional.ofNullable(
                datastore.find(StripeConfigDocument.class)
                        .filter(Filters.eq("_id", StripeConfigDocument.DOC_ID))
                        .first());
    }

    @Override
    public void saveConfig(StripeConfigDocument document) {
        datastore.save(document);
    }

}
