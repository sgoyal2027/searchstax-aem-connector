package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullIndexReportMessageFormatterTest {

    @Test
    void formatSuccessMessage_withoutBatchNumber() {
        assertEquals(
                "Successfully indexed during full reindex",
                FullIndexReportMessageFormatter.formatSuccessMessage(0, 500L));
    }

    @Test
    void formatSuccessMessage_withBatchAndDuration() {
        assertEquals(
                "Successfully indexed to SearchStax in full reindex batch 3 (1200 ms)",
                FullIndexReportMessageFormatter.formatSuccessMessage(3, 1200L));
    }

    @Test
    void formatSuccessMessage_withBatchWithoutDuration() {
        assertEquals(
                "Successfully indexed to SearchStax in full reindex batch 7",
                FullIndexReportMessageFormatter.formatSuccessMessage(7, 0L));
    }

    @Test
    void formatSuccessMessageFromStored_reusesExistingMessage() {
        final String stored = "Successfully indexed to SearchStax in full reindex batch 2 (50 ms)";

        assertEquals(
                stored,
                FullIndexReportMessageFormatter.formatSuccessMessageFromStored(stored, "batch-2", 50L));
    }

    @Test
    void formatSuccessMessageFromStored_buildsFromBatchId() {
        assertEquals(
                "Successfully indexed to SearchStax in full reindex batch 4 (80 ms)",
                FullIndexReportMessageFormatter.formatSuccessMessageFromStored(null, "batch-4", 80L));
    }

    @Test
    void formatFailureMessage_batchFailureIncludesRetryAndStatus() {
        final String message =
                FullIndexReportMessageFormatter.formatFailureMessage(
                        "batch-5", 429, "Too Many Requests", 6);

        assertTrue(message.contains("batch 5"));
        assertTrue(message.contains("6 retry attempt(s)"));
        assertTrue(message.contains("HTTP 429 Too Many Requests"));
        assertTrue(message.contains("Too Many Requests"));
    }

    @Test
    void formatFailureMessage_pathPayloadLimitFailure() {
        final String message =
                FullIndexReportMessageFormatter.formatFailureMessage(
                        "path-payload-limit-_content_wknd_large",
                        413,
                        "payload too large",
                        0);

        assertTrue(message.contains("batch payload size limit"));
        assertTrue(message.contains("payload too large"));
        assertFalse(message.contains("retry"));
    }

    @Test
    void formatFailureMessage_pathSerializeFailureUsesStatusWhenDetailMissing() {
        final String message =
                FullIndexReportMessageFormatter.formatFailureMessage(
                        "path-serialize-_content_page", 500, "", 0);

        assertTrue(message.contains("serialize"));
        assertTrue(message.contains("HTTP 500 Internal Server Error"));
    }

    @Test
    void formatFailureMessage_pathResolverAndBuildKinds() {
        assertTrue(
                FullIndexReportMessageFormatter.formatFailureMessage("path-resolver-x", 0, "", 0)
                        .contains("read content from the repository"));
        assertTrue(
                FullIndexReportMessageFormatter.formatFailureMessage("path-build-x", 0, "", 0)
                        .contains("build the search document"));
        assertTrue(
                FullIndexReportMessageFormatter.formatFailureMessage("path-document-limit-x", 0, "", 0)
                        .contains("per-document size limit"));
    }

    @Test
    void formatFailureMessage_truncatesVeryLongErrorDetail() {
        final String longError = "x".repeat(400);
        final String message =
                FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 400, longError, 0);

        assertTrue(message.endsWith("..."));
        assertTrue(message.length() < longError.length());
    }

    @Test
    void formatFailureMessage_coversCommonHttpStatusLabels() {
        assertTrue(
                FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 401, "", 0)
                        .contains("HTTP 401 Unauthorized"));
        assertTrue(
                FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 503, "", 0)
                        .contains("HTTP 503 Service Unavailable"));
        assertTrue(
                FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 599, "", 0)
                        .contains("HTTP 599 Network or transport error"));
        assertTrue(
                FullIndexReportMessageFormatter.formatFailureMessage("batch-1", 418, "", 0)
                        .contains("HTTP 418"));
    }

    @Test
    void parseBatchNumber_readsFromStoredMessage() {
        assertEquals(
                9,
                FullIndexReportMessageFormatter.parseBatchNumber(
                        null, "Successfully indexed to SearchStax in full reindex batch 9 (10 ms)"));
    }
}
