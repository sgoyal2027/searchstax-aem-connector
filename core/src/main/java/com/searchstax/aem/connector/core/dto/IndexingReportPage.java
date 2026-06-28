package com.searchstax.aem.connector.core.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IndexingReportPage {

    private final List<Map<String, Object>> events;
    private final int totalCount;

    public IndexingReportPage(final List<Map<String, Object>> events, final int totalCount) {
        this.events = events != null ? events : Collections.emptyList();
        this.totalCount = Math.max(totalCount, 0);
    }

    public List<Map<String, Object>> getEvents() {
        return events;
    }

    public int getTotalCount() {
        return totalCount;
    }
}
