package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullIndexReportMessageFormatterTest {

    @Test
    void formatSuccessMessage_includesBatchAndDuration() {
        final String message = FullIndexReportMessageFormatter.formatSuccessMessage(12, 850L);

        assertTrue(message.contains("batch 12"));
        assertTrue(message.contains("850 ms"));
        assertTrue(message.contains("Successfully posted to SearchStax"));
    }

    @Test
    void formatSuccessMessageFromStored_upgradesLegacyMessage() {
        final String message = FullIndexReportMessageFormatter.formatSuccessMessageFromStored(
                "Indexed in batch 3", "batch-3", 120L);

        assertTrue(message.contains("batch 3"));
        assertTrue(message.contains("Successfully posted to SearchStax"));
    }

    @Test
    void formatFailureMessage_batchFailure_isReadable() {
        final String message = FullIndexReportMessageFormatter.formatFailureMessage(
                "batch-7",
                503,
                "Service Unavailable",
                5);

        assertTrue(message.contains("Full reindex batch 7 failed to post to SearchStax"));
        assertTrue(message.contains("after 5 retry attempt(s)"));
        assertTrue(message.contains("HTTP 503 Service Unavailable"));
        assertTrue(message.contains("Service Unavailable"));
    }

    @Test
    void formatFailureMessage_pathSerialize_isReadable() {
        final String message = FullIndexReportMessageFormatter.formatFailureMessage(
                "path-serialize-_content_wknd_page",
                422,
                "Failed to serialize document",
                0);

        assertTrue(message.contains("Could not serialize the page or asset"));
        assertTrue(message.contains("Failed to serialize document"));
    }

    @Test
    void formatFailureMessage_pathDocumentLimit_isReadable() {
        final String message = FullIndexReportMessageFormatter.formatFailureMessage(
                "path-document-limit-_content_large",
                413,
                "Document payload 12000 bytes exceeds 10240 byte limit",
                0);

        assertTrue(message.contains("per-document size limit"));
        assertTrue(message.contains("12000 bytes"));
    }
}
