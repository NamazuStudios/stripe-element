package dev.getelements.elements.stripe;

import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecord;
import dev.getelements.elements.stripe.model.PostingRecordResponse;
import dev.getelements.elements.stripe.service.MeterPostingService;
import dev.getelements.elements.stripe.model.StripeMode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link MeterPostingService} for integration tests: collects posting records so
 * tests can assert on what {@code recordMeterEvent} auto-recorded, without any datastore.
 */
public class MeterPostingServiceStub implements MeterPostingService {

    public final List<PostingRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void recordPostingOutcome(PostingRecord record) {
        records.add(record);
    }

    @Override
    public PostingRecordResponse listPostings(
            String identifierPrefix,
            String customerId,
            String eventName,
            PostingOutcome outcome,
            StripeMode mode,
            int limit,
            int offset) {
        final var all = records.stream()
                .filter(r -> outcome == null || r.outcome() == outcome)
                .toList();
        return new PostingRecordResponse(all, all.size(), false);
    }

}
