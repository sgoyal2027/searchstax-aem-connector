package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxFullIndexRetryPolicyTest {

    @Test
    void isRetryable_includes429And503() {
        assertTrue(SearchStaxFullIndexRetryPolicy.isRetryable(429));
        assertTrue(SearchStaxFullIndexRetryPolicy.isRetryable(503));
        assertTrue(SearchStaxFullIndexRetryPolicy.isRetryable(500));
        assertTrue(SearchStaxFullIndexRetryPolicy.isRetryable(502));
        assertTrue(SearchStaxFullIndexRetryPolicy.isRetryable(504));
    }

    @Test
    void isNonRetryable_includes400And413() {
        assertTrue(SearchStaxFullIndexRetryPolicy.isNonRetryable(400));
        assertTrue(SearchStaxFullIndexRetryPolicy.isNonRetryable(401));
        assertTrue(SearchStaxFullIndexRetryPolicy.isNonRetryable(403));
        assertTrue(SearchStaxFullIndexRetryPolicy.isNonRetryable(404));
        assertTrue(SearchStaxFullIndexRetryPolicy.isNonRetryable(413));
    }

    @Test
    void isRetryable_falseForNonRetryable4xx() {
        assertFalse(SearchStaxFullIndexRetryPolicy.isRetryable(400));
        assertFalse(SearchStaxFullIndexRetryPolicy.isRetryable(401));
        assertFalse(SearchStaxFullIndexRetryPolicy.isRetryable(413));
    }

    @Test
    void backoffMillis_exponentialFrom700ms() {
        assertEquals(700L, SearchStaxFullIndexRetryPolicy.backoffMillis(1));
        assertEquals(1400L, SearchStaxFullIndexRetryPolicy.backoffMillis(2));
        assertEquals(2800L, SearchStaxFullIndexRetryPolicy.backoffMillis(3));
    }

    @Test
    void maxPostAttempts_isFive() {
        assertEquals(5, SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS);
    }
}
