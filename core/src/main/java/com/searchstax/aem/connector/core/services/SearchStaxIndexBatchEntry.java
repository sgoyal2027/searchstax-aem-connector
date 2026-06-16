package com.searchstax.aem.connector.core.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * One document queued for a full-index batch POST.
 */
public final class SearchStaxIndexBatchEntry {

    private final Map<String, Object> document;
    private final String path;
    private final boolean page;
    private final String json;
    private final int bytes;

    public SearchStaxIndexBatchEntry(
            final Map<String, Object> document,
            final String path,
            final boolean page,
            final String json,
            final int bytes) {
        this.document = document == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(document));
        this.path = path;
        this.page = page;
        this.json = json;
        this.bytes = bytes;
    }

    public Map<String, Object> getDocument() {
        return Collections.unmodifiableMap(document);
    }

    public String getPath() {
        return path;
    }

    public boolean isPage() {
        return page;
    }

    public String getJson() {
        return json;
    }

    public int getBytes() {
        return bytes;
    }
}
