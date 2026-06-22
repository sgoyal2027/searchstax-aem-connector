package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
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
        final int limit = parseInt(request.getParameter("limit"), 500);
        final int retentionHours = parseInt(request.getParameter("retentionHours"), DEFAULT_RETENTION_HOURS);

        final List<Map<String, Object>> events;
        try {
            if (TYPE_FULL.equalsIgnoreCase(type)) {
                events = listFullIndexEvents(request, limit, retentionHours);
            } else {
                events = listIncrementalEvents(request, limit);
            }
        } catch (IOException e) {
            LOG.error("Unable to load indexing report events. type={}", type, e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Unable to load indexing report\"}");
            return;
        }

        final Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("type", TYPE_FULL.equalsIgnoreCase(type) ? TYPE_FULL : "incremental");
        payload.put("events", events);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(payload));
    }

    private List<Map<String, Object>> listIncrementalEvents(
            final SlingHttpServletRequest request, final int limit) {
        final String status = request.getParameter("status");
        final String action = request.getParameter("action");
        final boolean excludeQueued = parseBoolean(request.getParameter("excludeQueued"), true);
        return indexingAuditService.listEvents(status, action, excludeQueued, limit);
    }

    private List<Map<String, Object>> listFullIndexEvents(
            final SlingHttpServletRequest request, final int limit, final int retentionHours) throws IOException {
        final String status = request.getParameter("status");
        final String failureKind = request.getParameter("failureKind");

        final List<Map<String, Object>> events = new ArrayList<>();
        events.addAll(fullIndexAuditService.listEventsForReport(status, limit, retentionHours));
        events.addAll(fullIndexFailureStore.listFailureEventsForReport(status, limit, retentionHours));

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
        if (filtered.size() <= limit) {
            return filtered;
        }
        return new ArrayList<>(filtered.subList(0, limit));
    }

    private static boolean matchesFailureKindFilter(final String failureKind, final Map<String, Object> event) {
        if ("SUCCESS".equalsIgnoreCase(String.valueOf(event.get("status")))) {
            return "BATCH".equalsIgnoreCase(failureKind);
        }
        return failureKind.equalsIgnoreCase(String.valueOf(event.get("failureKind")));
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
