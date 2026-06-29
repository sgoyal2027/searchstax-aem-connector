package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/indexing-report-reprocess",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST
        })
public class IndexingReportReprocessServlet extends SlingAllMethodsServlet {

    private static final Gson GSON = new Gson();

    @Reference
    private transient IndexingAuditService indexingAuditService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final String path = request.getParameter("path");
        if (path == null || path.isBlank()) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"path is required\"}");
            return;
        }

        indexingAuditService.reprocessFailedPath(path);

        final Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("path", path);

        response.setContentType("application/json");
        response.getWriter().write(GSON.toJson(payload));
    }
}
