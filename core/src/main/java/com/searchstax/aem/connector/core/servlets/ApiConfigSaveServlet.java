package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.impl.ApiConfigServiceImpl;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=SearchStax API Config Save Servlet",
                "sling.servlet.methods=POST",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/api-config-save"
        })
public class ApiConfigSaveServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ApiConfigSaveServlet.class);

    private static final String[] PASSWORD_FIELDS = {
            "apiToken",
            "selectToken",
            "updateToken",
            "discoveryApiKey",
            "analyticsTrackingKey",
            "analyticsReportingApiKey"
    };

    @Reference
    private transient ResolverUtil resolverUtil;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        LOG.info("API configuration save request started");

        if (!validateRequiredFields(request, response)) {
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource resource = resolver.getResource(ApiConfigServiceImpl.CONFIG_PATH);
            if (resource == null) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Configuration path not found");
                return;
            }

            final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
            if (properties == null) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to update configuration");
                return;
            }

            final ValueMapReader existing = new ValueMapReader(resource.getValueMap());

            putTextProperty(properties, "endpointUrl", request.getParameter("endpointUrl"));
            putTextProperty(properties, "selectEndpoint", request.getParameter("selectEndpoint"));
            putTextProperty(properties, "updateEndpoint", request.getParameter("updateEndpoint"));
            putTextProperty(properties, "autoSuggestApi", request.getParameter("autoSuggestApi"));
            putTextProperty(properties, "relatedSearchesEndpoint", request.getParameter("relatedSearchesEndpoint"));
            putTextProperty(properties, "popularSearchesEndpoint", request.getParameter("popularSearchesEndpoint"));
            putTextProperty(properties, "analyticsTrackingUrl", request.getParameter("analyticsTrackingUrl"));
            putTextProperty(properties, "analyticsReportingUrl", request.getParameter("analyticsReportingUrl"));
            putTextProperty(properties, "forwardGeocodingEndpoint", request.getParameter("forwardGeocodingEndpoint"));
            putTextProperty(properties, "reverseGeocodingEndpoint", request.getParameter("reverseGeocodingEndpoint"));

            for (final String passwordField : PASSWORD_FIELDS) {
                putPasswordProperty(properties, existing, passwordField, request.getParameter(passwordField));
            }

            resolver.commit();
            LOG.info("API configuration saved at {}", ApiConfigServiceImpl.CONFIG_PATH);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\":true,\"message\":\"API configuration saved successfully.\"}");
        } catch (PersistenceException e) {
            LOG.error("Persistence error while saving API configuration", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to save configuration");
        } catch (Exception e) {
            LOG.error("Unexpected error while saving API configuration", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error occurred");
        }
    }

    private boolean validateRequiredFields(
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws IOException {
        if (isBlank(request.getParameter("endpointUrl"))
                || isBlank(request.getParameter("selectEndpoint"))
                || isBlank(request.getParameter("updateEndpoint"))) {
            respondBadRequest(response, "Endpoint URL, Select Endpoint, and Update Endpoint are required.");
            return false;
        }
        return true;
    }

    private void putTextProperty(
            final ModifiableValueMap properties,
            final String key,
            final String value) {
        final String trimmed = trimToEmpty(value);
        if (trimmed.isEmpty()) {
            properties.remove(key);
        } else {
            properties.put(key, trimmed);
        }
    }

    private void putPasswordProperty(
            final ModifiableValueMap properties,
            final ValueMapReader existing,
            final String key,
            final String posted) {
        final String trimmed = trimToEmpty(posted);
        if (!trimmed.isEmpty()) {
            properties.put(key, protect(trimmed));
            return;
        }
        final String existingValue = existing.getString(key);
        if (!existingValue.isEmpty()) {
            properties.put(key, existingValue);
        } else {
            properties.remove(key);
        }
    }

    private String protect(final String plaintext) {
        if (cryptoSupport == null) {
            return plaintext;
        }
        try {
            return cryptoSupport.protect(plaintext);
        } catch (CryptoException e) {
            LOG.warn("Failed to encrypt value for storage; storing plaintext", e);
            return plaintext;
        }
    }

    private static void respondBadRequest(final SlingHttpServletResponse response, final String message)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    private static String trimToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(final String value) {
        return trimToEmpty(value).isEmpty();
    }

    private static final class ValueMapReader {
        private final org.apache.sling.api.resource.ValueMap valueMap;

        private ValueMapReader(final org.apache.sling.api.resource.ValueMap valueMap) {
            this.valueMap = valueMap;
        }

        private String getString(final String key) {
            return valueMap.get(key, "");
        }
    }
}
