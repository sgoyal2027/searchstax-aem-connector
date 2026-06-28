package com.searchstax.aem.connector.core.services;

/**
 * Background full-index execution: traversal, batching, Solr push, in-memory progress.
 */
public interface SearchStaxFullIndexExecutionService {

    /**
     * Runs a full index job (single-threaded). Called from {@link com.searchstax.aem.connector.core.jobs.SearchStaxFullIndexJobConsumer}.
     */
    void execute();

    /**
     * Runs a full index job using the given path configuration.
     */
    void execute(FullIndexPathConfig pathConfig);

    /**
     * Clears prior run metrics when a new job is queued so status polling does not show stale data.
     */
    void prepareForQueuedJob();

    FullIndexProgress getProgressSnapshot();
}
