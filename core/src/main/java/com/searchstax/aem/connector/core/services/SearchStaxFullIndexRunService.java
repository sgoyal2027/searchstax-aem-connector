package com.searchstax.aem.connector.core.services;

/**
 * Facade for triggering async full-index runs.
 */
public interface SearchStaxFullIndexRunService {

    /**
     * Queues a background full-index job (returns immediately).
     */
    FullIndexTriggerResult triggerFullIndex(FullIndexPathConfig config);

    FullIndexProgress getProgress();
}
