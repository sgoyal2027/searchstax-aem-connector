package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;

public interface FailedRequestService {

    void saveFailedRequest(
            IndexRequest request,
            String reason,
            ApiResponse response);
}
