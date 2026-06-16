package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.wizard.SearchStaxWizardBindingPaths;
import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Triggers async full-index job (immediate 202 Accepted or 409 Conflict).
 */
@Component(
        immediate = true,
        service = Servlet.class,
        property = {
            Constants.SERVICE_DESCRIPTION + "=SearchStax full index async run",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
            ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SearchStaxWizardBindingPaths.SERVLET_FULL_INDEX_RUN,
            Constants.SERVICE_RANKING + ":Integer=200000"
        })
public class SearchStaxFullIndexRunServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxFullIndexRunServlet.class);

    @Reference
    private transient SearchStaxFullIndexRunService searchStaxFullIndexRunService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        LOG.info("Full index run requested");
        final FullIndexPathConfig config = FullIndexPathConfig.fromRequest(request);
        final FullIndexTriggerResult result = searchStaxFullIndexRunService.triggerFullIndex(config);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setStatus(result.getHttpStatus());

        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("accepted", result.isAccepted());
        body.put("success", result.isAccepted());
        body.put("jobId", result.getJobId());
        body.put("message", result.getMessage());

        new ObjectMapper().writeValue(response.getWriter(), body);
        LOG.info(
                "Full index trigger response: accepted={}, jobId={}, httpStatus={}",
                result.isAccepted(),
                result.getJobId(),
                result.getHttpStatus());
    }
}
