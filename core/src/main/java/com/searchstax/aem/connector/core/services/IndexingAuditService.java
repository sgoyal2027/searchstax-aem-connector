package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.IndexingReportPage;

import java.util.List;
import java.util.Map;

/**
 * Persists incremental indexing audit events for the admin report (24h retention).
 */
public interface IndexingAuditService {

    String STATUS_QUEUED = "QUEUED";
    String STATUS_SUCCESS = "SUCCESS";
    String STATUS_FAILURE = "FAILURE";
    String STATUS_SKIPPED = "SKIPPED";

    void recordEvent(
            String path,
            String action,
            String status,
            String message,
            String correlationId,
            long durationMs,
            String batchId);

    List<Map<String, Object>> listEvents(String statusFilter, String actionFilter, boolean excludeQueued, int maxResults);

    IndexingReportPage listEventsPaged(
            String statusFilter,
            String actionFilter,
            boolean excludeQueued,
            int offset,
            int pageSize);

    void purgeOlderThanHours(int hours);

    /**
     * Removes all incremental indexing audit events shown in the report.
     *
     * @return number of events removed
     */
    int clearAllEvents();

    void reprocessFailedPath(String path);
}
