package com.searchstax.aem.connector.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Serializes and validates full-index documents before batching (full-index pipeline only).
 */
public final class SearchStaxFullIndexDocumentSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxFullIndexDocumentSerializer.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Optional<SearchStaxFullIndexSerializedDocument> serialize(final Map<String, Object> document) {
        if (document == null) {
            return Optional.empty();
        }
        try {
            final String json = OBJECT_MAPPER.writeValueAsString(document);
            final int bytes = json.getBytes(StandardCharsets.UTF_8).length;
            return Optional.of(new SearchStaxFullIndexSerializedDocument(json, bytes));
        } catch (final JsonProcessingException e) {
            LOG.warn("Failed to serialize document for size validation", e);
            return Optional.empty();
        } catch (final StackOverflowError e) {
            LOG.warn("Failed to serialize document due to circular reference", e);
            return Optional.empty();
        }
    }

    public boolean isWithinDocumentLimit(final int documentBytes) {
        return documentBytes >= 0 && documentBytes <= SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES;
    }

    public boolean isWithinDocumentLimit(final SearchStaxFullIndexSerializedDocument serialized) {
        return serialized != null && isWithinDocumentLimit(serialized.getBytes());
    }
}
