package com.searchstax.aem.connector.core.services;

/**
 * In-memory snapshot of a running or completed full-index job (Phase 1).
 */
public final class FullIndexProgress {

    public enum State {
        IDLE,
        RUNNING,
        /** All flushed batches indexed successfully. */
        SUCCESS,
        /** Traversal completed but one or more batches failed POST (no replay in same run). */
        PARTIAL_FAILURE,
        /** Catastrophic failure; traversal did not complete normally. */
        FAILED
    }

    private final State state;
    private final long totalProcessed;
    private final long successCount;
    private final long failureCount;
    private final long pagesIndexed;
    private final long assetsIndexed;
    private final int currentBatchNumber;
    private final String lastIndexedPath;
    private final long startedAt;
    private final long elapsedMs;
    private final String message;

    public FullIndexProgress(
            final State state,
            final long totalProcessed,
            final long successCount,
            final long failureCount,
            final long pagesIndexed,
            final long assetsIndexed,
            final int currentBatchNumber,
            final String lastIndexedPath,
            final long startedAt,
            final long elapsedMs,
            final String message) {
        this.state = state;
        this.totalProcessed = totalProcessed;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.pagesIndexed = pagesIndexed;
        this.assetsIndexed = assetsIndexed;
        this.currentBatchNumber = currentBatchNumber;
        this.lastIndexedPath = lastIndexedPath == null ? "" : lastIndexedPath;
        this.startedAt = startedAt;
        this.elapsedMs = elapsedMs;
        this.message = message == null ? "" : message;
    }

    public State getState() {
        return state;
    }

    /**
     * Documents successfully indexed to SearchStax (same as {@link #getSuccessCount()}).
     */
    public long getTotalProcessed() {
        return totalProcessed;
    }

    public long getSuccessCount() {
        return successCount;
    }

    /**
     * Paths that were indexed successfully plus paths that failed during collection (payload limit, serialize, etc.).
     * Does not include documents in failed HTTP batches ({@code failedBatchCount} is separate).
     */
    public long getTotalAttempted() {
        return successCount + failureCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public long getPagesIndexed() {
        return pagesIndexed;
    }

    public long getAssetsIndexed() {
        return assetsIndexed;
    }

    public int getCurrentBatchNumber() {
        return currentBatchNumber;
    }

    public String getLastIndexedPath() {
        return lastIndexedPath;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getMessage() {
        return message;
    }
}
