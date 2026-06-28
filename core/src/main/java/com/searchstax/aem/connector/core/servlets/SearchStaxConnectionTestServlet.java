package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/staxsync/searchstax/test-connection",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        }
)
public class SearchStaxConnectionTestServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SearchStaxConnectionTestServlet.class);
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    @Reference
    private CryptoSupport cryptoSupport;

    @Reference
    private transient ApiConfigService apiConfigService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {

        log.info("[SearchStax] doPost invoked on /bin/staxsync/searchstax/test-connection");

        final String endpointUrl = trimToEmpty(request.getParameter("endpointUrl"));
        final String endpointType = trimToEmpty(request.getParameter("endpointType"));
        String incomingToken = trimToEmpty(request.getParameter("apiToken"));

        if (isBlank(incomingToken) && apiConfigService != null) {
            final ApiConfig savedConfig = apiConfigService.getConfiguration();
            if (savedConfig != null) {
                if ("general".equals(endpointType)) {
                    incomingToken = savedConfig.getApiToken();
                } else if ("searchSelect".equals(endpointType)) {
                    incomingToken = savedConfig.getSelectToken();
                } else if ("searchUpdate".equals(endpointType)) {
                    incomingToken = savedConfig.getUpdateToken();
                } else {
                    incomingToken = savedConfig.getDiscoveryApiKey();
                }
            }
        }

        log.debug("[SearchStax] Processing connection test: endpointUrl={}, endpointType={}", endpointUrl, endpointType);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");

        if (isBlank(endpointUrl) || isBlank(incomingToken)) {
            log.warn("[SearchStax] Missing connection parameters — endpointUrl blank={}, token blank={}",
                    isBlank(endpointUrl), isBlank(incomingToken));
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Missing required parameters.\"}");
            return;
        }

        // Decrypt the token natively using AEM Granite Crypto Support
        String tokenToUseForTest = unprotectTokenIfNeeded(incomingToken);

        // Hand over execution to the appropriate connection probe layout
        if ("searchSelect".equals(endpointType) || "searchUpdate".equals(endpointType) || "general".equals(endpointType)) {
            log.info("[SearchStax] Routing connection challenge to testSearchEndpoint");
            testSearchEndpoint(response, endpointUrl, endpointType, tokenToUseForTest);
        } else {
            log.info("[SearchStax] Routing connection challenge to testDiscoveryEndpoint");
            testDiscoveryEndpoint(response, endpointUrl, endpointType, tokenToUseForTest);
        }
    }

    /**
     * Replicates the backend unprotect logic used within SearchStaxConfigurationServiceImpl
     */
    private String unprotectTokenIfNeeded(final String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (cryptoSupport == null) {
            log.warn("[SearchStax] CryptoSupport OSGi service reference is unavailable. Using raw token value.");
            return value;
        }
        try {
            if (cryptoSupport.isProtected(value)) {
                log.info("[SearchStax] Protected token detected. Unprotecting value natively via AEM Granite...");
                return cryptoSupport.unprotect(value);
            }
        } catch (final CryptoException e) {
            log.error("[SearchStax] Native decryption failed; parsing fallback state", e);
        }
        return value;
    }

    private void testSearchEndpoint(final SlingHttpServletResponse response,
                                    final String endpointUrl,
                                    final String endpointType,
                                    final String token) throws IOException {

        if (!isHttpUrl(endpointUrl)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Endpoint URL must start with http:// or https://.\"}");
            return;
        }

        final String normalizedEndpoint = endpointUrl.replaceAll("/+$", "");
        final String probeUrl = resolveSearchProbeUrl(normalizedEndpoint, endpointType);
        HttpURLConnection connection = null;
        
        try {
            connection = openAuthorizedConnection(probeUrl, token, DEFAULT_TIMEOUT_MS);
            final int targetStatus = connection.getResponseCode();
            readBody(targetStatus, connection); // Fully drain the input stream buffer
            final boolean success = targetStatus >= 200 && targetStatus < 300;

            response.setStatus(success ? SlingHttpServletResponse.SC_OK : SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{"
                    + "\"success\":" + success + ","
                    + "\"targetStatus\":" + targetStatus + ","
                    + "\"testUrl\":\"" + escapeJson(probeUrl) + "\","
                    + "\"message\":\"" + escapeJson(mapSearchSuccessOrFailure(endpointType, success, targetStatus)) + "\""
                    + "}");
        } catch (IOException ex) {
            log.error("[SearchStax] testSearchEndpoint failed for url={}: {}", endpointUrl, ex.getMessage());
            response.setStatus(SlingHttpServletResponse.SC_BAD_GATEWAY);
            response.getWriter().write("{\"success\":false,\"targetStatus\":0,\"message\":\"Unable to connect: " + escapeJson(ex.getMessage()) + "\"}");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void testDiscoveryEndpoint(final SlingHttpServletResponse response,
                                       final String endpointUrl,
                                       final String endpointType,
                                       final String apiKey) throws IOException {
        final String normalizedEndpoint = endpointUrl.replaceAll("/+$", "");
        HttpURLConnection connection = null;
        try {
            final URL url = new URL(normalizedEndpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HttpConstants.METHOD_GET);
            connection.setConnectTimeout(DEFAULT_TIMEOUT_MS);
            connection.setReadTimeout(DEFAULT_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            if (!isBlank(apiKey)) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            final int targetStatus = connection.getResponseCode();
            final boolean success = targetStatus >= 200 && targetStatus < 300;
            final String message = success ? mapDiscoverySuccessMessage(endpointType) : mapFailureMessage(targetStatus);

            response.setStatus(success ? SlingHttpServletResponse.SC_OK : SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{"
                    + "\"success\":" + success + ","
                    + "\"targetStatus\":" + targetStatus + ","
                    + "\"message\":\"" + escapeJson(message) + "\""
                    + "}");
        } catch (IOException ex) {
            log.error("[SearchStax] testDiscoveryEndpoint failed for url={}: {}", endpointUrl, ex.getMessage());
            response.setStatus(SlingHttpServletResponse.SC_BAD_GATEWAY);
            response.getWriter().write("{\"success\":false,\"message\":\"Connection failed.\"}");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readBody(final int statusCode, final HttpURLConnection connection) throws IOException {
        final InputStream stream = statusCode >= SlingHttpServletResponse.SC_BAD_REQUEST
                ? connection.getErrorStream()
                : connection.getInputStream();
        if (stream == null) return "";
        final StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String mapFailureMessage(final int status) {
        if (status == 401 || status == 403) return "Invalid or unauthorized token.";
        if (status == 404) return "Incorrect Host URL or context path.";
        return "Connection test failed with HTTP status " + status + ".";
    }

    private String mapSearchSuccessOrFailure(final String endpointType, final boolean success, final int status) {
        String label = "searchUpdate".equals(endpointType) ? "Update Endpoint" : "Configuration";
        if (success) return "Connection successful.";
        return label + " test failed: " + mapFailureMessage(status);
    }

    private String mapDiscoverySuccessMessage(final String endpointType) {
        return "Endpoint is reachable.";
    }

    private String escapeJson(final String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isHttpUrl(final String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String resolveSearchProbeUrl(final String normalizedEndpoint, final String endpointType) {
        if ("searchUpdate".equals(endpointType)) return normalizedEndpoint;
        return normalizedEndpoint.endsWith("/emselect") ? normalizedEndpoint + "?q=*&rows=1" : normalizedEndpoint + "/emselect?q=*&rows=1";
    }

    private HttpURLConnection openAuthorizedConnection(final String endpoint, final String token, final int timeout) throws IOException {
        final URL url = new URL(endpoint);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(HttpConstants.METHOD_GET);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestProperty("Authorization", "Token " + token);
        return connection;
    }
}