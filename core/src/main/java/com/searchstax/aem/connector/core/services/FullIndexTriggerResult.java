package com.searchstax.aem.connector.core.services;

/**
 * Immediate HTTP response when admin triggers full index (async job queued or rejected).
 */
public final class FullIndexTriggerResult {

    private final boolean accepted;
    private final String jobId;
    private final String message;
    private final int httpStatus;

    public FullIndexTriggerResult(
            final boolean accepted,
            final String jobId,
            final String message,
            final int httpStatus) {
        this.accepted = accepted;
        this.jobId = jobId == null ? "" : jobId;
        this.message = message == null ? "" : message;
        this.httpStatus = httpStatus;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getJobId() {
        return jobId;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
