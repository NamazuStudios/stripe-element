package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.stripe.dao.MeterPostingDao;
import dev.getelements.elements.stripe.model.MeterPostingDocument;
import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecord;
import dev.getelements.elements.stripe.model.PostingRecordResponse;
import dev.getelements.elements.stripe.model.StripeMode;

import java.time.Instant;

@ElementServiceExport(MeterPostingService.class)
public class MeterPostingServiceImpl implements MeterPostingService {

    private final MeterPostingDao postingDao;

    @Inject
    public MeterPostingServiceImpl(MeterPostingDao postingDao) {
        this.postingDao = postingDao;
    }

    @Override
    public void recordPostingOutcome(PostingRecord record) {

        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }

        if (record.identifier() == null || record.identifier().isBlank()) {
            throw new IllegalArgumentException("identifier (the idempotency key) is required");
        }

        if (record.outcome() == null) {
            throw new IllegalArgumentException("outcome is required");
        }

        final var doc = new MeterPostingDocument();
        doc.setIdentifier(record.identifier());
        doc.setCustomerId(record.customerId());
        doc.setEventName(record.eventName());
        doc.setQuantity(record.quantity() != null ? record.quantity().toPlainString() : null);
        doc.setOutcome(record.outcome().name());
        doc.setMode(record.mode() != null ? record.mode().name() : null);
        doc.setDetail(record.detail());
        doc.setCreatedAt(record.at() != null && record.at() > 0 ? record.at() : System.currentTimeMillis());
        postingDao.save(doc);
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

        final var records = postingDao.findPostings(
                        identifierPrefix, customerId, eventName, outcome, mode, limit, offset).stream()
                .map(d -> new PostingRecord(
                        d.getIdentifier(),
                        d.getCustomerId(),
                        d.getEventName(),
                        d.getQuantity() != null ? new java.math.BigDecimal(d.getQuantity()) : null,
                        parseOutcome(d.getOutcome()),
                        parseMode(d.getMode()),
                        d.getDetail(),
                        d.getCreatedAt()))
                .toList();

        final var total = postingDao.countPostings(identifierPrefix, customerId, eventName, outcome, mode);
        return new PostingRecordResponse(records, total, (long) offset + records.size() < total);
    }

    private static PostingOutcome parseOutcome(String name) {
        try {
            return name != null ? PostingOutcome.valueOf(name) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static StripeMode parseMode(String name) {
        try {
            return name != null ? StripeMode.valueOf(name) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
