package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 * Processes incremental index/delete batches against SearchStax.
 */
public interface IncrementalIndexingService {

    /**
     * Processes a batch of queued requests and removes successfully handled items from the queue.
     */
    void processBatch(ResourceResolver resolver, List<IndexRequest> batch);
}
