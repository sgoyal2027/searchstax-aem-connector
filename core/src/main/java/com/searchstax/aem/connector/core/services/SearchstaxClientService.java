package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.SearchStaxUpdateOptions;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;

public interface SearchstaxClientService {

    ApiResponse indexDocument(String requestJson);

    ApiResponse indexDocument(String requestJson, String contentPath);

    ApiResponse deleteDocument(String payload);

    ApiResponse deleteDocument(String payload, String contentPath);

    ApiResponse postUpdate(String requestJson, SearchStaxUpdateOptions options);
}
