package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxFullIndexDocumentSerializerTest {

    private final SearchStaxFullIndexDocumentSerializer serializer = new SearchStaxFullIndexDocumentSerializer();

    @Test
    void serialize_returnsJsonAndUtf8ByteCount() {
        final Map<String, Object> document = Map.of("id", "/content/test", "title", "Hello");

        final Optional<SearchStaxFullIndexSerializedDocument> result = serializer.serialize(document);

        assertTrue(result.isPresent());
        assertEquals(
                result.get().getJson().getBytes(StandardCharsets.UTF_8).length,
                result.get().getBytes());
    }

    @Test
    void isWithinDocumentLimit_acceptsAt80KbBoundary() {
        final int atLimit = SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES;
        final int overLimit = SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES + 1;

        assertTrue(serializer.isWithinDocumentLimit(atLimit));
        assertFalse(serializer.isWithinDocumentLimit(overLimit));
    }

    @Test
    void serialize_returnsEmptyForNonSerializableValue() {
        final Map<String, Object> document = new HashMap<>();
        document.put("id", "/content/bad");
        document.put("self", document);

        assertTrue(serializer.serialize(document).isEmpty());
    }

    @Test
    void isWithinDocumentLimit_usesSerializedDocumentBytes() {
        final Map<String, Object> document = Map.of("id", "/content/small");
        final SearchStaxFullIndexSerializedDocument serialized =
                serializer.serialize(document).orElseThrow();

        assertTrue(serializer.isWithinDocumentLimit(serialized));
    }
}
