package com.searchstax.aem.connector.core.services;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;

import java.util.List;

public interface IncrementalQueueService {

    default void addRequest(final String path, final ReplicationActionType actionType) {
        addRequest(path, actionType, null);
    }

    void addRequest(String path, ReplicationActionType actionType, String correlationId);

    List<IndexRequest> getBatch(int batchSize);

    void removeProcessed(List<IndexRequest> requests);

    void updateRequest(IndexRequest request);

    int size();

    void incrementRetryCount(IndexRequest request);
}
