package com.searchstax.aem.connector.core.services;

/**
 * Human-readable messages for the full reindex indexing report.
 */
public final class FullIndexReportMessageFormatter {

    private static final int MAX_DETAIL_CHARS = 300;

    private FullIndexReportMessageFormatter() {
    }

    public static String formatSuccessMessage(final int batchNumber, final long durationMs) {
        if (batchNumber <= 0) {
            return "Successfully indexed during full reindex";
        }
        if (durationMs > 0) {
            return String.format(
                    "Successfully indexed to SearchStax in full reindex batch %d (%d ms)",
                    batchNumber,
                    durationMs);
        }
        return String.format("Successfully indexed to SearchStax in full reindex batch %d", batchNumber);
    }

    public static String formatSuccessMessageFromStored(
            final String storedMessage, final String batchId, final long durationMs) {
        final int batchNumber = parseBatchNumber(batchId, storedMessage);
        if (storedMessage != null
                && (storedMessage.startsWith("Successfully ")
                        || storedMessage.contains("SearchStax"))) {
            return storedMessage;
        }
        return formatSuccessMessage(batchNumber, durationMs);
    }

    public static String formatFailureMessage(
            final String batchId,
            final int statusCode,
            final String errorMessage,
            final int retryAttempts) {
        if (isPathFailure(batchId)) {
            return formatPathFailureMessage(batchId, statusCode, errorMessage);
        }
        return formatBatchFailureMessage(batchId, statusCode, errorMessage, retryAttempts);
    }

    private static String formatPathFailureMessage(
            final String batchId, final int statusCode, final String errorMessage) {
        final StringBuilder message = new StringBuilder(describePathFailure(batchId));
        final String detail = normalizeDetail(errorMessage);
        if (!detail.isEmpty()) {
            message.append(": ").append(detail);
        } else if (statusCode > 0) {
            message.append(" (").append(httpStatusLabel(statusCode)).append(")");
        }
        return message.toString();
    }

    private static String formatBatchFailureMessage(
            final String batchId,
            final int statusCode,
            final String errorMessage,
            final int retryAttempts) {
        final int batchNumber = parseBatchNumber(batchId, null);
        final StringBuilder message = new StringBuilder();
        if (batchNumber > 0) {
            message.append("Full reindex batch ").append(batchNumber).append(" failed to post to SearchStax");
        } else {
            message.append("Full reindex batch failed to post to SearchStax");
        }
        if (retryAttempts > 0) {
            message.append(" after ").append(retryAttempts).append(" retry attempt(s)");
        }
        if (statusCode > 0) {
            message.append(" (").append(httpStatusLabel(statusCode)).append(")");
        }
        final String detail = normalizeDetail(errorMessage);
        if (!detail.isEmpty()) {
            message.append(": ").append(detail);
        }
        return message.toString();
    }

    private static String describePathFailure(final String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return "Path failed during full reindex";
        }
        if (batchId.startsWith("path-serialize")) {
            return "Could not serialize the page or asset into a search document";
        }
        if (batchId.startsWith("path-document-limit")) {
            return "Document exceeds the SearchStax per-document size limit";
        }
        if (batchId.startsWith("path-payload-limit")) {
            return "Document exceeds the full reindex batch payload size limit";
        }
        if (batchId.startsWith("path-resolver")) {
            return "Could not read content from the repository for full reindex";
        }
        if (batchId.startsWith("path-build")) {
            return "Could not build the search document for this path";
        }
        return "Path failed during full reindex";
    }

    private static boolean isPathFailure(final String batchId) {
        return batchId != null && batchId.startsWith("path-");
    }

    static int parseBatchNumber(final String batchId, final String storedMessage) {
        final Integer fromBatchId = parsePrefixedNumber(batchId, "batch-");
        if (fromBatchId != null) {
            return fromBatchId;
        }
        if (storedMessage != null) {
            final Integer fromMessage = parseIndexedInBatchNumber(storedMessage);
            if (fromMessage != null) {
                return fromMessage;
            }
        }
        return -1;
    }

    private static Integer parsePrefixedNumber(final String value, final String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            return null;
        }
        final int end = value.indexOf('-', prefix.length());
        final String numberPart = end > prefix.length() ? value.substring(prefix.length(), end) : value.substring(prefix.length());
        try {
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseIndexedInBatchNumber(final String storedMessage) {
        final String marker = "batch ";
        final int index = storedMessage.indexOf(marker);
        if (index < 0) {
            return null;
        }
        final int start = index + marker.length();
        int end = start;
        while (end < storedMessage.length() && Character.isDigit(storedMessage.charAt(end))) {
            end++;
        }
        if (end == start) {
            return null;
        }
        try {
            return Integer.parseInt(storedMessage.substring(start, end));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalizeDetail(final String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "";
        }
        final String trimmed = errorMessage.trim();
        if (trimmed.length() <= MAX_DETAIL_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_DETAIL_CHARS) + "...";
    }

    private static String httpStatusLabel(final int statusCode) {
        switch (statusCode) {
            case 400:
                return "HTTP 400 Bad Request";
            case 401:
                return "HTTP 401 Unauthorized - verify API token";
            case 403:
                return "HTTP 403 Forbidden - verify endpoint access";
            case 404:
                return "HTTP 404 Not Found - verify SearchStax endpoint or collection";
            case 413:
                return "HTTP 413 Payload Too Large";
            case 422:
                return "HTTP 422 Unprocessable Entity";
            case 429:
                return "HTTP 429 Too Many Requests";
            case 500:
                return "HTTP 500 Internal Server Error";
            case 502:
                return "HTTP 502 Bad Gateway";
            case 503:
                return "HTTP 503 Service Unavailable";
            case 599:
                return "HTTP 599 Network or transport error";
            default:
                if (statusCode > 0) {
                    return "HTTP " + statusCode;
                }
                return "Unknown HTTP status";
        }
    }
}
