package com.searchstax.aem.connector.core.services.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexingHelperServiceImplTest {

    private final IndexingHelperServiceImpl helper = new IndexingHelperServiceImpl();

    @Test
    void cleanText_removesHtmlTagsAndNormalizesWhitespace() {
        assertEquals("Hello World", helper.cleanText("<p>Hello   World</p>"));
    }

    @Test
    void cleanText_replacesNbspWithSpace() {
        assertEquals("foo bar", helper.cleanText("foo&nbsp;bar"));
    }

    @Test
    void cleanText_returnsEmptyStringForNull() {
        assertEquals("", helper.cleanText(null));
    }

    @Test
    void shouldRetry_returnsFalseFor401And403() {
        assertFalse(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(401, "")));
        assertFalse(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(403, "")));
    }

    @Test
    void shouldRetry_returnsTrueFor429UnlessPlanLimitExceeded() {
        assertTrue(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(429, "Too Many Requests")));
        assertFalse(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(429, "Plan Limit Exceeded")));
    }

    @Test
    void isPermanentFailure_includes413() {
        assertTrue(helper.isPermanentFailure(new com.searchstax.aem.connector.core.dto.response.ApiResponse(413, "")));
    }

    @Test
    void formatFailureMessage_permanentFailure_includesHttpStatusAndBody() {
        final String message = helper.formatFailureMessage(
                "PERMANENT_FAILURE",
                new com.searchstax.aem.connector.core.dto.response.ApiResponse(
                        401, "{\"error\":\"invalid token\"}"),
                null);

        assertTrue(message.contains("HTTP 401 Unauthorized"));
        assertTrue(message.contains("invalid token"));
        assertTrue(message.contains("will not be retried"));
    }

    @Test
    void formatFailureMessage_planLimitExceeded_isReadable() {
        final String message = helper.formatFailureMessage(
                "PLAN_LIMIT_EXCEEDED",
                new com.searchstax.aem.connector.core.dto.response.ApiResponse(
                        429, "Plan Limit Exceeded"),
                null);

        assertTrue(message.contains("plan document limit exceeded"));
        assertTrue(message.contains("HTTP 429"));
    }
}
