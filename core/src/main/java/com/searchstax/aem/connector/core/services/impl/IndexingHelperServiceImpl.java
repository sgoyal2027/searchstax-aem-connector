package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRetryPolicy;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component(service = IndexingHelperService.class)
public class IndexingHelperServiceImpl
        implements IndexingHelperService {

    private static final Logger LOG =
            LoggerFactory.getLogger(IndexingHelperServiceImpl.class);

    private static final long BASE_DELAY_MS =
            700L;

    private static final int MAX_CONTENT_LENGTH = 100000;

    private static final Set<String> TEXT_EXTRACTABLE_MIME_TYPES =
            Set.of(
                    "text/plain",
                    "text/csv",
                    "text/tab-separated-values",
                    "application/json"
            );


    @Reference
    private MetadataFieldConfigService metadataFieldConfigService;

    @Reference
    private InitialSetupConfigService initialSetupConfigService;

    @Override
    public boolean isSuccess(ApiResponse response) {

        if (response == null) {
            return false;
        }

        int statusCode = response.getStatusCode();

        if (statusCode < 200 || statusCode >= 300) {
            return false;
        }

        String responseBody = response.getResponseBody();

        if (responseBody != null
                && responseBody.contains("\"success\":false")) {

            return false;
        }

        return true;
    }

    @Override
    public boolean shouldRetry(
            ApiResponse response) {

        if (response == null) {
            return true;
        }

        if (isPlanLimitExceeded(response)) {
            return false;
        }

        final int statusCode = response.getStatusCode();

        if (SearchStaxFullIndexRetryPolicy.isNonRetryable(statusCode)) {
            return false;
        }

        if (statusCode == 429) {
            return true;
        }

        return SearchStaxFullIndexRetryPolicy.isRetryable(statusCode);
    }

    @Override
    public boolean isPlanLimitExceeded(
            ApiResponse response) {

        if (response == null) {
            return false;
        }

        return response.getStatusCode() == 429
                && response.getResponseBody()
                != null
                && response.getResponseBody()
                .contains(
                        "Plan Limit Exceeded");
    }

    @Override
    public long calculateDelay(
            int attempt) {

        return BASE_DELAY_MS
                * (1L << (attempt - 1));
    }

    @Override
    public String extractText(Asset asset) {

        LOG.info(
                "Starting text extraction for asset: {}",
                asset.getPath());

        long startTime = System.currentTimeMillis();

        try {

            Rendition original =
                    asset.getOriginal();

            if (original == null) {
                return "";
            }

            if (!isTextExtractableAsset(asset)) {

                LOG.info(
                        "Skipping text extraction. Path={} MimeType={}",
                        asset.getPath(),
                        asset.getMimeType());

                return "";
            }

            try (InputStream inputStream = original.getStream()) {

                final byte[] bytes = inputStream.readAllBytes();
                String extractedText = new String(bytes, StandardCharsets.UTF_8);

                if (extractedText.length() > MAX_CONTENT_LENGTH) {
                    extractedText = extractedText.substring(0, MAX_CONTENT_LENGTH);
                }

                LOG.info(
                        "Extracted {} characters from asset {} in {} ms",
                        extractedText.length(),
                        asset.getPath(),
                        System.currentTimeMillis() - startTime);

                return extractedText;
            }

        } catch (Exception e) {

            LOG.error("Error extracting asset text", e);
        }

        return "";
    }

    public void addField(Map<String, Object> document,
                          String fieldName,
                          Object value) {

        if (value == null) {
            return;
        }

        document.put(fieldName, value);
    }

    @Override
    public String cleanText(String text) {

        if (text == null) {
            return "";
        }

        // Remove HTML tags
        text = text.replaceAll("<[^>]*>", " ");

        // Remove HTML entities
        text = text.replace("&nbsp;", " ");

        // Normalize whitespace
        text = text.replaceAll("\\s+", " ")
                .trim();

        return text;
    }

    @Override
    public void addConfiguredMetadataFields(
            Map<String, Object> document,
            ValueMap valueMap) {

        List<MetadataFieldMappingConfig> mappings =
                metadataFieldConfigService
                        .getMetadataFieldMappings();

        if (mappings == null || mappings.isEmpty()) {

            LOG.warn("No metadata field mappings configured");

            return;
        }

        LOG.info(
                "Processing {} metadata field mappings",
                mappings.size());

        for (MetadataFieldMappingConfig mapping : mappings) {

            if (!mapping.isEnabled()) {
                continue;
            }

            String sourceProperty =
                    mapping.getCustomProperty() != null
                            && !mapping.getCustomProperty()
                            .trim()
                            .isEmpty()
                            ? mapping.getCustomProperty()
                            : mapping.getAemField();

            if (sourceProperty == null
                    || sourceProperty.trim().isEmpty()
                    || mapping.getSearchStaxField() == null
                    || mapping.getSearchStaxField()
                    .trim()
                    .isEmpty()) {

                LOG.warn(
                        "Skipping invalid mapping. aemField={}, customProperty={}, searchStaxField={}",
                        mapping.getAemField(),
                        mapping.getCustomProperty(),
                        mapping.getSearchStaxField());

                continue;
            }

            Object value = valueMap.get(sourceProperty);

            if (value == null) {

                LOG.debug(
                        "Property '{}' not found",
                        sourceProperty);

                continue;
            }

            addField(
                    document,
                    mapping.getSearchStaxField(),
                    value);

            LOG.debug(
                    "Mapped '{}' -> '{}'",
                    sourceProperty,
                    mapping.getSearchStaxField());
        }
    }

    @Override
    public Object normalizeMetadataValue(Object value) {

        if (value instanceof Calendar) {

            return ((Calendar) value)
                    .toInstant()
                    .toString();
        }

        return value;
    }

    @Override
    public String resolveFieldName(
            String baseField,
            String fieldType,
            Object value,
            String language) {

        if (baseField.matches(
                ".*_(txt|txt_[a-z]{2}|txts_[a-z]{2}|ss|dt|dts|b|l|d|s)$")) {

            return baseField;
        }

        if (fieldType != null
            && !fieldType.trim().isEmpty()) {

            switch (fieldType.toLowerCase()) {

                case "text":
                    return baseField + "_txt_" + language;

                case "strings":
                    return baseField + "_ss_" + language;

                case "string":
                    return baseField + "_s_" + language;

                case "int":
                case "long":
                    return baseField + "_l";

                case "float":
                case "double":
                    return baseField + "_d";

                case "boolean":
                    return baseField + "_b";

                case "date":
                    return baseField + "_dt";
            }
        }

        if (value instanceof Calendar
                || value instanceof Date) {
            return baseField + "_dt";
        }

        if (value instanceof String[]) {
            return baseField + "_ss";
        }

        if (value instanceof Boolean) {
            return baseField + "_b";
        }

        if (value instanceof Integer
                || value instanceof Long) {
            return baseField + "_l";
        }

        if (value instanceof Float
                || value instanceof Double) {
            return baseField + "_d";
        }

        return baseField + "_txt_" + language;
    }

    @Override
    public String detectLanguage(String text) {
//        if (text == null || text.trim().isEmpty()) {
//
//            return "en";
//        }
//
//        try {
//            OptimaizeLangDetector detector =
//                    new OptimaizeLangDetector();
//
//            detector.loadModels();
//
//            LanguageResult result =
//                    detector.detect(text);
//
//            return result.isReasonablyCertain()
//                    ? result.getLanguage()
//                    : "en";
//
//        } catch (Exception e) {
//
//            LOG.warn(
//                    "Unable to detect language",
//                    e);
//
//            return "en";
//        }
        return "en";
    }

    @Override
    public boolean isSupportedAsset(Asset asset) {

        if (asset == null) {
            return false;
        }

        InitialSetupConfig config =
                initialSetupConfigService
                        .getConfiguration();

        String[] allowedFiles =
                config.getAllowedFiles();

        if (allowedFiles == null
                || allowedFiles.length == 0) {

            return true;
        }

        String assetName =
                asset.getName();

        int lastDot =
                assetName.lastIndexOf('.');

        if (lastDot < 0) {

            return false;
        }

        String extension =
                assetName.substring(lastDot + 1);

        for (String allowedFile : allowedFiles) {

            if (allowedFile.equalsIgnoreCase(
                    extension)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPermanentFailure(
            ApiResponse response) {

        if (response == null) {
            return false;
        }

        int status =
                response.getStatusCode();

        return status == 400
                || status == 401
                || status == 403
                || status == 404
                || status == 413;
    }

    @Override
    public boolean isTextExtractableAsset(
            Asset asset) {

        String mimeType =
                asset.getMimeType();

        return mimeType != null
                && TEXT_EXTRACTABLE_MIME_TYPES.contains(
                mimeType.toLowerCase());
    }

}