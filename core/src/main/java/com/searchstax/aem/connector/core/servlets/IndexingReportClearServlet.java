package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.services.FullIndexAuditService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexFailureStore;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/indexing-report-clear",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST
        })
public class IndexingReportClearServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingReportClearServlet.class);
    private static final Gson GSON = new Gson();
    private static final String TYPE_FULL = "full";

    @Reference
    private transient IndexingAuditService indexingAuditService;

    @Reference
    private transient FullIndexAuditService fullIndexAuditService;

    @Reference
    private transient SearchStaxFullIndexFailureStore fullIndexFailureStore;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final String type = request.getParameter("type");
        final Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("type", TYPE_FULL.equalsIgnoreCase(type) ? TYPE_FULL : "incremental");

        try {
            if (TYPE_FULL.equalsIgnoreCase(type)) {
                final int successEvents = fullIndexAuditService.clearAllEvents();
                final int failureEvents = fullIndexFailureStore.clearAllFailures();
                payload.put("removedCount", successEvents + failureEvents);
                payload.put("removedSuccessEvents", successEvents);
                payload.put("removedFailureEvents", failureEvents);
            } else {
                final int removedCount = indexingAuditService.clearAllEvents();
                payload.put("removedCount", removedCount);
            }
        } catch (Exception e) {
            LOG.error("Unable to clear indexing report. type={}", type, e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Unable to clear indexing report\"}");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(payload));
    }
}
