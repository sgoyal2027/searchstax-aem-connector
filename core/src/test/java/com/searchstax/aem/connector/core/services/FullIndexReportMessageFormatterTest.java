package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullIndexReportMessageFormatterTest {

    @Test
    void formatSuccessMessage_includesBatchAndDuration() {
        final String message = FullIndexReportMessageFormatter.formatSuccessMessage(12, 850L);

        assertTrue(message.contains("batch 12"));
        assertTrue(message.contains("850 ms"));
        assertTrue(message.contains("Successfully indexed to SearchStax"));
    }

    @Test
    void formatSuccessMessageFromStored_upgradesLegacyMessage() {
        final String message = FullIndexReportMessageFormatter.formatSuccessMessageFromStored(
                "Indexed in batch 3", "batch-3", 120L);

        assertTrue(message.contains("batch 3"));
        assertTrue(message.contains("Successfully indexed to SearchStax"));
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

    @Test
    void formatSuccessMessage_withoutBatchNumber_usesGenericMessage() {
        assertEquals("Successfully indexed during full reindex", FullIndexReportMessageFormatter.formatSuccessMessage(0, 100L));
    }

    @Test
    void formatSuccessMessage_withoutDuration_omitsMilliseconds() {
        final String message = FullIndexReportMessageFormatter.formatSuccessMessage(4, 0L);

        assertTrue(message.contains("batch 4"));
        assertTrue(message.contains("Successfully indexed to SearchStax"));
        assertFalse(message.contains("ms"));
    }

    @Test
    void formatSuccessMessageFromStored_keepsAlreadyFormattedMessage() {
        final String stored = "Successfully indexed to SearchStax in full reindex batch 2 (90 ms)";

        assertEquals(stored, FullIndexReportMessageFormatter.formatSuccessMessageFromStored(stored, "batch-2", 90L));
    }

    @Test
    void formatFailureMessage_pathPayloadLimit_isReadable() {
        final String message = FullIndexReportMessageFormatter.formatFailureMessage(
                "path-payload-limit-_content_batch",
                413,
                "Batch payload exceeds limit",
                0);

        assertTrue(message.contains("batch payload size limit"));
        assertTrue(message.contains("Batch payload exceeds limit"));
    }

    @Test
    void formatFailureMessage_pathResolver_isReadable() {
        final String message = FullIndexReportMessageFormatter.formatFailureMessage(
                "path-resolver-_content_missing",
                404,
                "",
                0);

        assertTrue(message.contains("Could not read content from the repository"));
        assertTrue(message.contains("HTTP 404 Not Found"));
    }

    @Test
    void formatFailureMessage_pathBuild_isReadable() {
        final String message = FullIndexReportMessageFormatter.formatFailureMessage(
                "path-build-_content_page",
                0,
                "Missing required metadata",
                0);

        assertTrue(message.contains("Could not build the search document"));
        assertTrue(message.contains("Missing required metadata"));
    }

    @Test
    void formatFailureMessage_truncatesVeryLongDetail() {
        final String longDetail = "x".repeat(350);
        final String message = FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 500, longDetail, 1);

        assertTrue(message.length() < longDetail.length() + 80);
        assertTrue(message.endsWith("..."));
    }

    @Test
    void formatFailureMessage_batchFailure_mapsCommonHttpStatuses() {
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-2", 401, "", 0)
                .contains("HTTP 401 Unauthorized"));
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-2", 429, "", 0)
                .contains("HTTP 429 Too Many Requests"));
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-2", 599, "", 0)
                .contains("HTTP 599 Network or transport error"));
    }

    @Test
    void formatFailureMessage_mapsAdditionalHttpStatuses() {
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 400, "", 0)
                .contains("HTTP 400 Bad Request"));
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 403, "", 0)
                .contains("HTTP 403 Forbidden"));
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 500, "", 0)
                .contains("HTTP 500 Internal Server Error"));
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 502, "", 0)
                .contains("HTTP 502 Bad Gateway"));
        assertTrue(FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 418, "", 0)
                .contains("HTTP 418"));
    }

    @Test
    void formatFailureMessage_genericPathFailure_usesFallbackDescription() {
        final String message =
                FullIndexReportMessageFormatter.formatFailureMessage("path-other", 500, "failed", 0);

        assertTrue(message.contains("Path failed during full reindex"));
        assertTrue(message.contains("failed"));
    }

    @Test
    void formatFailureMessage_batchWithoutNumber_usesGenericBatchMessage() {
        final String message =
                FullIndexReportMessageFormatter.formatFailureMessage("batch", 503, "down", 2);

        assertTrue(message.contains("Full reindex batch failed to post to SearchStax"));
        assertTrue(message.contains("after 2 retry attempt(s)"));
    }

    @Test
    void formatSuccessMessageFromStored_parsesBatchNumberFromLegacyMessage() {
        final String message = FullIndexReportMessageFormatter.formatSuccessMessageFromStored(
                "Indexed in batch 7", "batch-unknown", 250L);

        assertTrue(message.contains("batch 7"));
    }
}
