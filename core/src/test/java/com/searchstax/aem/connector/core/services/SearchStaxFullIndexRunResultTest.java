package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxFullIndexRunResultTest {

    @Test
    void constructor_exposesAllFields() {
        final SearchStaxFullIndexRunResult result =
                new SearchStaxFullIndexRunResult(true, "Indexed", 200, 5, 3, "{\"ok\":true}");

        assertTrue(result.isSuccess());
        assertEquals("Indexed", result.getMessage());
        assertEquals(200, result.getHttpStatus());
        assertEquals(5, result.getPagesIndexed());
        assertEquals(3, result.getAssetsIndexed());
        assertEquals("{\"ok\":true}", result.getSolrResponseBody());
    }

    @Test
    void constructor_normalizesNullMessageAndBody() {
        final SearchStaxFullIndexRunResult result =
                new SearchStaxFullIndexRunResult(false, null, 500, 0, 0, null);

        assertFalse(result.isSuccess());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSolrResponseBody());
    }
}
