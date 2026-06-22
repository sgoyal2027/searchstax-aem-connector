package com.searchstax.aem.connector.core.services;

import com.day.cq.dam.api.Asset;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import org.apache.sling.api.resource.ValueMap;

import java.util.Map;

public interface IndexingHelperService {

    boolean isSuccess(ApiResponse response);

    boolean shouldRetry(ApiResponse response);

    boolean isPlanLimitExceeded(ApiResponse response);

    long calculateDelay(int attempt);

    String extractText(Asset asset);


    void addField(Map<String, Object> document,
                          String fieldName,
                          Object value);

    String cleanText(String text);

    void addConfiguredMetadataFields(
            Map<String, Object> document,
            ValueMap valueMap);

    Object normalizeMetadataValue(Object value);

    String resolveFieldName(
            String baseField,
            String fieldType,
            Object value,
            String language);

    String detectLanguage(String text);

    boolean isSupportedAsset(Asset asset);

    boolean isPermanentFailure(ApiResponse response);

    /**
     * Builds a user-facing incremental indexing failure message for the audit report.
     */
    String formatFailureMessage(String reasonCode, ApiResponse response, Exception cause);

    boolean isTextExtractableAsset(Asset asset);

}
