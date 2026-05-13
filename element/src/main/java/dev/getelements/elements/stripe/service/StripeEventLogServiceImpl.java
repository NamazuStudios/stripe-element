package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.stripe.dao.StripeEventLogDao;
import dev.getelements.elements.stripe.model.StripeEventLogDocument;
import dev.getelements.elements.stripe.model.StripeEventLogEntry;
import dev.getelements.elements.stripe.model.StripeEventLogResponse;

import java.time.Instant;

@ElementServiceExport(StripeEventLogService.class)
public class StripeEventLogServiceImpl implements StripeEventLogService {

    private final StripeEventLogDao eventLogDao;

    @Inject
    public StripeEventLogServiceImpl(StripeEventLogDao eventLogDao) {
        this.eventLogDao = eventLogDao;
    }

    @Override
    public void logEvent(String stripeEventId, String eventType) {
        final var doc = new StripeEventLogDocument();
        doc.setStripeEventId(stripeEventId);
        doc.setEventType(eventType);
        doc.setReceivedAt(System.currentTimeMillis());
        eventLogDao.save(doc);
    }

    @Override
    public StripeEventLogResponse listEvents(String type, int limit, int offset) {

        final var events = eventLogDao.findEvents(type, limit, offset).stream()
                .map(d -> new StripeEventLogEntry(
                        d.getStripeEventId(),
                        d.getEventType(),
                        Instant.ofEpochMilli(d.getReceivedAt()).toString()))
                .toList();

        final var total = eventLogDao.countEvents(type);
        return new StripeEventLogResponse(events, total, (long) offset + events.size() < total);
    }

}
