package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxIndexBatchEntryTest {

    @Test
    void constructor_nullDocument_usesEmptyMap() {
        final SearchStaxIndexBatchEntry entry =
                new SearchStaxIndexBatchEntry(null, "/content/a", true, "{}", 2);

        assertTrue(entry.getDocument().isEmpty());
        assertEquals("/content/a", entry.getPath());
        assertTrue(entry.isPage());
        assertEquals("{}", entry.getJson());
        assertEquals(2, entry.getBytes());
    }

    @Test
    void getDocument_returnsUnmodifiableCopy() {
        final Map<String, Object> source = new HashMap<>();
        source.put("id", "/content/a");

        final SearchStaxIndexBatchEntry entry =
                new SearchStaxIndexBatchEntry(source, "/content/a", false, "{}", 2);

        assertThrows(UnsupportedOperationException.class, () -> entry.getDocument().put("x", "y"));
        source.put("extra", "value");
        assertFalse(entry.getDocument().containsKey("extra"));
    }

    @Test
    void getters_returnConstructorValues() {
        final Map<String, Object> document = Map.of("id", "/content/dam/asset.jpg");
        final SearchStaxIndexBatchEntry entry =
                new SearchStaxIndexBatchEntry(document, "/content/dam/asset.jpg", false, "{\"id\":\"x\"}", 12);

        assertEquals("/content/dam/asset.jpg", entry.getPath());
        assertFalse(entry.isPage());
        assertEquals("{\"id\":\"x\"}", entry.getJson());
        assertEquals(12, entry.getBytes());
        assertEquals("/content/dam/asset.jpg", entry.getDocument().get("id"));
    }
}
