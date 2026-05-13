package dev.getelements.elements.stripe.model;

import java.util.List;

public record StripeEventLogResponse(List<StripeEventLogEntry> events, long total, boolean hasMore) {}
