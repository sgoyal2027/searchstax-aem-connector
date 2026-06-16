package com.searchstax.aem.connector.core.services;

/**
 * Queues async full-index jobs and enforces single active run.
 */
public interface SearchStaxFullIndexOrchestratorService {

    FullIndexTriggerResult triggerFullIndex(FullIndexPathConfig config);

    FullIndexProgress getProgress();
}
