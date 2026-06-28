package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;
import com.searchstax.aem.connector.core.dto.SearchStaxUpdateOptions;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import com.searchstax.aem.connector.core.services.SiteRoutingService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component(service = SearchstaxClientService.class)
public class SearchstaxClientServiceImpl implements SearchstaxClientService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchstaxClientServiceImpl.class);

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 120_000;
    static final int TRANSPORT_ERROR_STATUS = 599;

    @Reference
    private ApiConfigService apiConfigService;

    @Reference
    private SiteRoutingService siteRoutingService;

    @Reference
    private ProtectedValueCodec protectedValueCodec;

    @Override
    public ApiResponse indexDocument(final String requestJson) {
        return postUpdate(requestJson, SearchStaxUpdateOptions.incrementalDefault());
    }

    @Override
    public ApiResponse indexDocument(final String requestJson, final String contentPath) {
        return postUpdate(requestJson, optionsForPath(contentPath, true));
    }

    @Override
    public ApiResponse deleteDocument(final String payload) {
        return postUpdate(payload, SearchStaxUpdateOptions.incrementalDefault());
    }

    @Override
    public ApiResponse deleteDocument(final String payload, final String contentPath) {
        return postUpdate(payload, optionsForPath(contentPath, true));
    }

    @Override
    public ApiResponse postUpdate(final String requestJson, final SearchStaxUpdateOptions options) {
        HttpURLConnection connection = null;
        try {
            final String endpoint = resolveEndpoint(options);
            final String token = resolveToken(options);
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalStateException("SearchStax update endpoint is not configured");
            }
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("SearchStax update token is not configured");
            }

            final String requestUrl = buildRequestUrl(endpoint, options);
            LOG.info("Sending SearchStax update request to {}", sanitizeUrlForLog(requestUrl));
            LOG.info("SearchStax Request Payload:\n{}", requestJson);
            LOG.info("SearchStax Payload Size={} bytes", requestJson.getBytes(StandardCharsets.UTF_8).length);

            final URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Authorization", "Token " + token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestJson.getBytes(StandardCharsets.UTF_8));
            }

            final int responseCode = connection.getResponseCode();
            final InputStream responseStream =
                    responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            final String responseBody = readResponse(responseStream);

            LOG.info("SearchStax response code: {}", responseCode);
            LOG.debug("SearchStax response body: {}", responseBody);

            return new ApiResponse(responseCode, responseBody);
        } catch (Exception e) {
            LOG.error("Error while calling SearchStax update API", e);
            return new ApiResponse(TRANSPORT_ERROR_STATUS, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private SearchStaxUpdateOptions optionsForPath(final String contentPath, final boolean hardCommit) {
        final SiteRoutingResult routing = siteRoutingService.resolve(contentPath);
        return SearchStaxUpdateOptions.routed(
                hardCommit,
                null,
                routing.getUpdateEndpoint(),
                routing.getUpdateToken(),
                routing.getSearchProfile());
    }

    private String resolveEndpoint(final SearchStaxUpdateOptions options) {
        if (options.getUpdateEndpoint() != null && !options.getUpdateEndpoint().isBlank()) {
            return options.getUpdateEndpoint();
        }
        return apiConfigService.getConfiguration().getUpdateEndpoint();
    }

    private String resolveToken(final SearchStaxUpdateOptions options) {
        final String raw = options.getUpdateToken() != null && !options.getUpdateToken().isBlank()
                ? options.getUpdateToken()
                : apiConfigService.getConfiguration().getUpdateToken();
        return protectedValueCodec.unprotectIfNeeded(raw);
    }

    static String buildRequestUrl(final String baseEndpoint, final SearchStaxUpdateOptions options) {
        final StringBuilder urlBuilder = new StringBuilder(baseEndpoint);
        boolean hasQuery = baseEndpoint.contains("?");

        if (options.getCommitWithinMs() != null) {
            urlBuilder.append(hasQuery ? '&' : '?')
                    .append("commitWithin=")
                    .append(options.getCommitWithinMs());
            hasQuery = true;
        }

        final String built = urlBuilder.toString();
        if (options.isHardCommit() && !built.contains("commit=true")) {
            urlBuilder.append(hasQuery ? '&' : '?').append("commit=true");
            hasQuery = true;
        }

        if (options.getSearchProfile() != null && !options.getSearchProfile().isBlank()) {
            urlBuilder.append(hasQuery ? '&' : '?')
                    .append("Model=")
                    .append(urlEncode(options.getSearchProfile()));
        }

        return urlBuilder.toString();
    }

    private static String urlEncode(final String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String sanitizeUrlForLog(final String url) {
        return url.replaceAll("Token [^&]+", "Token ***");
    }

    private static String readResponse(final InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        final StringBuilder response = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
