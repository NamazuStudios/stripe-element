package dev.getelements.elements.stripe.model;

import java.util.List;

/**
 * One page of {@link PostingRecord}s from the posting-record store, newest first.
 *
 * @param records  the page of records
 * @param total    total matching records across all pages
 * @param hasMore  whether another page follows
 */
public record PostingRecordResponse(List<PostingRecord> records, long total, boolean hasMore) {}
