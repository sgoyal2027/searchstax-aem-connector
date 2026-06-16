package com.searchstax.aem.connector.core.services;

/**
 * Pre-serialized full-index document payload (JSON fragment and UTF-8 byte length).
 */
public final class SearchStaxFullIndexSerializedDocument {

    private final String json;
    private final int bytes;

    public SearchStaxFullIndexSerializedDocument(final String json, final int bytes) {
        this.json = json;
        this.bytes = bytes;
    }

    public String getJson() {
        return json;
    }

    public int getBytes() {
        return bytes;
    }
}
