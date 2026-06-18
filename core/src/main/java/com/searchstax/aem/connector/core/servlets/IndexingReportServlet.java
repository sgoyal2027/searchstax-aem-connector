package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/indexing-report",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        })
public class IndexingReportServlet extends SlingSafeMethodsServlet {

    private static final Gson GSON = new Gson();

    @Reference
    private IndexingAuditService indexingAuditService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final String status = request.getParameter("status");
        final String action = request.getParameter("action");
        final boolean excludeQueued = parseBoolean(request.getParameter("excludeQueued"), true);
        final int limit = parseInt(request.getParameter("limit"), 500);

        final List<Map<String, Object>> events = indexingAuditService.listEvents(status, action, excludeQueued, limit);

        final Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("events", events);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(payload));
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
