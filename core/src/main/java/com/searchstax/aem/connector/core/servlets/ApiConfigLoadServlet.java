package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.utils.SecretFieldSupport;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=SearchStax API Config Load Servlet",
                "sling.servlet.methods=GET",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/api-config-load"
        })
public class ApiConfigLoadServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ApiConfigLoadServlet.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Reference
    private transient ApiConfigService apiConfigService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        try {
            final ApiConfig config = apiConfigService.getConfiguration();
            final ObjectNode json = OBJECT_MAPPER.createObjectNode();

            json.put("endpointUrl", nullToEmpty(config.getEndpointUrl()));
            json.put("selectEndpoint", nullToEmpty(config.getSelectEndpoint()));
            json.put("updateEndpoint", nullToEmpty(config.getUpdateEndpoint()));
            json.put("autoSuggestApi", nullToEmpty(config.getAutoSuggestApi()));
            json.put("relatedSearchesEndpoint", nullToEmpty(config.getRelatedSearchesEndpoint()));
            json.put("popularSearchesEndpoint", nullToEmpty(config.getPopularSearchesEndpoint()));
            json.put("analyticsTrackingUrl", nullToEmpty(config.getAnalyticsTrackingUrl()));
            json.put("analyticsReportingUrl", nullToEmpty(config.getAnalyticsReportingUrl()));
            json.put("forwardGeocodingEndpoint", nullToEmpty(config.getForwardGeocodingEndpoint()));
            json.put("reverseGeocodingEndpoint", nullToEmpty(config.getReverseGeocodingEndpoint()));

            // Secrets are never returned to the author UI; leave blank and preserve on save when unchanged.
            json.put("apiToken", "");
            json.put("apiTokenConfigured", SecretFieldSupport.isConfigured(config.getApiToken()));
            json.put("selectToken", "");
            json.put("selectTokenConfigured", SecretFieldSupport.isConfigured(config.getSelectToken()));
            json.put("updateToken", "");
            json.put("updateTokenConfigured", SecretFieldSupport.isConfigured(config.getUpdateToken()));
            json.put("discoveryApiKey", "");
            json.put("discoveryApiKeyConfigured", SecretFieldSupport.isConfigured(config.getDiscoveryApiKey()));
            json.put("analyticsTrackingKey", "");
            json.put("analyticsTrackingKeyConfigured", SecretFieldSupport.isConfigured(config.getAnalyticsTrackingKey()));
            json.put("analyticsReportingApiKey", "");
            json.put("analyticsReportingApiKeyConfigured", SecretFieldSupport.isConfigured(config.getAnalyticsReportingApiKey()));

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(json.toString());
        } catch (Exception e) {
            LOG.error("Error loading API configuration", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            final ObjectNode errorJson = OBJECT_MAPPER.createObjectNode();
            errorJson.put("error", "Unable to load configuration");
            response.getWriter().print(errorJson.toString());
        }
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
