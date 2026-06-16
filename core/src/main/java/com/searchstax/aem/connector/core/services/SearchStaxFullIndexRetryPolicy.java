package com.searchstax.aem.connector.core.services;

/**
 * Retry rules for full-index batch POST failures (transient HTTP statuses only).
 */
public final class SearchStaxFullIndexRetryPolicy {

    public static final int MAX_POST_ATTEMPTS = 5;

    public static final long BASE_BACKOFF_MS = 700L;

    private SearchStaxFullIndexRetryPolicy() {
    }

    public static boolean isRetryable(final int statusCode) {
        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }

    public static boolean isNonRetryable(final int statusCode) {
        return statusCode == 400
                || statusCode == 401
                || statusCode == 403
                || statusCode == 404
                || statusCode == 413;
    }

    /**
     * Exponential backoff for the given 1-based POST attempt index within the retry loop.
     * First retry (after attempt 1 fails) uses {@link #BASE_BACKOFF_MS}.
     */
    public static long backoffMillis(final int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
        return BASE_BACKOFF_MS * (1L << (attempt - 1));
    }
}
