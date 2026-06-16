package com.searchstax.aem.connector.core.services;

import java.util.List;

import com.searchstax.aem.connector.core.dto.request.IndexRequest;

public interface IndexFailureNotificationService {

    void sendFailureNotification(
            String batchId,
            List<IndexRequest> failedRequests);

}
