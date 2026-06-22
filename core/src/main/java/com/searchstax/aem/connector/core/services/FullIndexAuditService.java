package com.searchstax.aem.connector.core.services;

import java.util.List;
import java.util.Map;

/**
 * Persists per-path full reindex success events for the admin report (24h retention).
 */
public interface FullIndexAuditService {

    String ACTION_FULL_REINDEX = "FULL_REINDEX";
    String STATUS_SUCCESS = "SUCCESS";

    void recordSuccessBatch(List<String> paths, int batchNumber, long durationMs);

    List<Map<String, Object>> listEventsForReport(String statusFilter, int maxResults, int retentionHours);

    void purgeOlderThanHours(int hours);
}
