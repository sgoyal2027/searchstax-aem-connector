package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.dto.IndexingReportPage;
import com.searchstax.aem.connector.core.services.FullIndexAuditService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexFailureStore;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/indexing-report",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        })
public class IndexingReportServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingReportServlet.class);
    private static final Gson GSON = new Gson();
    private static final String TYPE_FULL = "full";
    private static final int DEFAULT_RETENTION_HOURS = 24;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int FULL_REPORT_FETCH_CAP = 10_000;

    @Reference
    private IndexingAuditService indexingAuditService;

    @Reference
    private SearchStaxFullIndexFailureStore fullIndexFailureStore;

    @Reference
    private FullIndexAuditService fullIndexAuditService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final String type = request.getParameter("type");
        final int page = Math.max(parseInt(request.getParameter("page"), DEFAULT_PAGE), DEFAULT_PAGE);
        final int pageSize = resolvePageSize(request);
        final int offset = (page - 1) * pageSize;
        final int retentionHours = parseInt(request.getParameter("retentionHours"), DEFAULT_RETENTION_HOURS);

        final IndexingReportPage reportPage;
        try {
            if (TYPE_FULL.equalsIgnoreCase(type)) {
                reportPage = listFullIndexEvents(request, offset, pageSize, retentionHours);
            } else {
                reportPage = listIncrementalEvents(request, offset, pageSize);
            }
        } catch (IOException e) {
            LOG.error("Unable to load indexing report events. type={}", type, e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Unable to load indexing report\"}");
            return;
        }

        final int totalCount = reportPage.getTotalCount();
        final int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);

        final Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("type", TYPE_FULL.equalsIgnoreCase(type) ? TYPE_FULL : "incremental");
        payload.put("events", reportPage.getEvents());
        payload.put("page", page);
        payload.put("pageSize", pageSize);
        payload.put("totalCount", totalCount);
        payload.put("totalPages", totalPages);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(payload));
    }

    private IndexingReportPage listIncrementalEvents(
            final SlingHttpServletRequest request, final int offset, final int pageSize) {
        final String status = request.getParameter("status");
        final String action = request.getParameter("action");
        final boolean excludeQueued = parseBoolean(request.getParameter("excludeQueued"), true);
        return indexingAuditService.listEventsPaged(status, action, excludeQueued, offset, pageSize);
    }

    private IndexingReportPage listFullIndexEvents(
            final SlingHttpServletRequest request,
            final int offset,
            final int pageSize,
            final int retentionHours) throws IOException {
        final String status = request.getParameter("status");
        final String failureKind = request.getParameter("failureKind");

        final List<Map<String, Object>> events = new ArrayList<>();
        events.addAll(fullIndexAuditService.listEventsForReport(status, FULL_REPORT_FETCH_CAP, retentionHours));
        events.addAll(fullIndexFailureStore.listFailureEventsForReport(status, FULL_REPORT_FETCH_CAP, retentionHours));

        List<Map<String, Object>> filtered = events;
        if (failureKind != null
                && !failureKind.isBlank()
                && !"ALL".equalsIgnoreCase(failureKind)) {
            filtered = events.stream()
                    .filter(event -> matchesFailureKindFilter(failureKind, event))
                    .collect(Collectors.toList());
        }

        filtered.sort(Comparator.comparing(
                event -> String.valueOf(event.getOrDefault("timestamp", "")),
                Comparator.reverseOrder()));

        final int totalCount = filtered.size();
        if (offset >= totalCount || pageSize <= 0) {
            return new IndexingReportPage(List.of(), totalCount);
        }

        final int endIndex = Math.min(offset + pageSize, totalCount);
        return new IndexingReportPage(new ArrayList<>(filtered.subList(offset, endIndex)), totalCount);
    }

    private static boolean matchesFailureKindFilter(final String failureKind, final Map<String, Object> event) {
        if ("SUCCESS".equalsIgnoreCase(String.valueOf(event.get("status")))) {
            return "BATCH".equalsIgnoreCase(failureKind);
        }
        return failureKind.equalsIgnoreCase(String.valueOf(event.get("failureKind")));
    }

    private static int resolvePageSize(final SlingHttpServletRequest request) {
        int pageSize = parseInt(request.getParameter("pageSize"), -1);
        if (pageSize <= 0) {
            pageSize = parseInt(request.getParameter("limit"), DEFAULT_PAGE_SIZE);
        }
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static int parseInt(final String value, final int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(final String value, final boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
