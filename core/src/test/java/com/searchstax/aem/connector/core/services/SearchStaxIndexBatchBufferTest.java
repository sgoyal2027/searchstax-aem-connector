package com.searchstax.aem.connector.core.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxIndexBatchBufferTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void calculateProjectedBytes_matchesJoinedArrayUtf8Length() throws Exception {
        final SearchStaxIndexBatchBuffer buffer = new SearchStaxIndexBatchBuffer();
        final List<Map<String, Object>> documents = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final Map<String, Object> doc = new HashMap<>();
            doc.put("id", "/content/page-" + i);
            doc.put("title", "Page " + i);
            documents.add(doc);

            final String json = OBJECT_MAPPER.writeValueAsString(doc);
            final int bytes = json.getBytes(StandardCharsets.UTF_8).length;

            assertFalse(buffer.wouldExceed(bytes));
            buffer.add(new SearchStaxIndexBatchEntry(doc, doc.get("id").toString(), true, json, bytes));
        }

        final String expectedJson = OBJECT_MAPPER.writeValueAsString(documents);
        final String actualJson = buffer.toJson();

        assertEquals(expectedJson, actualJson);
        assertEquals(
                expectedJson.getBytes(StandardCharsets.UTF_8).length,
                buffer.toJsonUtf8Bytes());
        assertEquals(
                expectedJson.getBytes(StandardCharsets.UTF_8).length,
                buffer.getPayloadBytes());
    }

    @Test
    void wouldExceed_returnsTrueWhenNextDocumentExceedsBatchLimit() throws Exception {
        final SearchStaxIndexBatchBuffer buffer = new SearchStaxIndexBatchBuffer();
        // Projected empty-batch size is 2 + docBytes; body must exceed MAX_BATCH_PAYLOAD_BYTES minus JSON overhead.
        final String largeJson = OBJECT_MAPPER.writeValueAsString(
                Map.of("body", "x".repeat(SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES)));
        final int largeBytes = largeJson.getBytes(StandardCharsets.UTF_8).length;

        assertTrue(buffer.wouldExceed(largeBytes));
    }

    @Test
    void clear_resetsEntriesAndPayloadBytes() throws Exception {
        final SearchStaxIndexBatchBuffer buffer = new SearchStaxIndexBatchBuffer();
        final String json = OBJECT_MAPPER.writeValueAsString(Map.of("id", "/content/a"));
        final int bytes = json.getBytes(StandardCharsets.UTF_8).length;

        buffer.add(new SearchStaxIndexBatchEntry(Map.of("id", "/content/a"), "/content/a", true, json, bytes));
        assertEquals(1, buffer.size());
        assertTrue(buffer.getPayloadBytes() > 0);

        buffer.clear();

        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.getPayloadBytes());
        assertEquals("[]", buffer.toJson());
    }
}
