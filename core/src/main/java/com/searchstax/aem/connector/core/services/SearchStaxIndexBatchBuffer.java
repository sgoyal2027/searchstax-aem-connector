package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates pre-serialized documents for full-index batch POSTs with running payload size tracking.
 */
public final class SearchStaxIndexBatchBuffer {

    private final List<SearchStaxIndexBatchEntry> entries = new ArrayList<>();
    private int payloadBytes;

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public int getPayloadBytes() {
        return payloadBytes;
    }

    public List<SearchStaxIndexBatchEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static int calculateProjectedBytes(final int currentPayloadBytes, final int documentBytes) {
        if (documentBytes < 0) {
            throw new IllegalArgumentException("documentBytes must be non-negative");
        }
        return (currentPayloadBytes == 0)
                ? (2 + documentBytes)
                : (currentPayloadBytes + documentBytes + 1);
    }

    public boolean wouldExceed(final int documentBytes) {
        return calculateProjectedBytes(payloadBytes, documentBytes) > SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES;
    }

    public void add(final SearchStaxIndexBatchEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null");
        }
        payloadBytes = calculateProjectedBytes(payloadBytes, entry.getBytes());
        entries.add(entry);
    }

    /**
     * Builds the JSON array body from pre-serialized document fragments (no second map serialization).
     */
    public String toJson() {
        if (entries.isEmpty()) {
            return "[]";
        }
        final StringBuilder builder = new StringBuilder(payloadBytes);
        builder.append('[');
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(entries.get(i).getJson());
        }
        builder.append(']');
        return builder.toString();
    }

    public int toJsonUtf8Bytes() {
        return toJson().getBytes(StandardCharsets.UTF_8).length;
    }

    public void clear() {
        entries.clear();
        payloadBytes = 0;
    }
}
