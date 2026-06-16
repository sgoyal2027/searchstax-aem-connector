package com.searchstax.aem.connector.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Runtime tuning for full-index execution (batching, traversal, Solr commit strategy).
 */
@ObjectClassDefinition(
        name = "SearchStax Connector — Full index runtime tuning",
        description = "Batch size, traversal mode, throttle, and commit settings for full reindex runs.")
public @interface SearchStaxFullIndexRuntimeConfiguration {

    @AttributeDefinition(name = "Batch size", description = "Documents per batch before flush")
    int batchSize() default 100;

    @AttributeDefinition(name = "Query page size", description = "Paths fetched per traversal page")
    int queryPageSize() default 500;

    @AttributeDefinition(
            name = "Traversal mode",
            description = "JCR_SQL2 (default) or QUERY_BUILDER after validation on target AEM")
    String traversalMode() default "JCR_SQL2";

    @AttributeDefinition(name = "Commit within (ms)", description = "Solr commitWithin parameter")
    long commitWithinMs() default 120000L;

    @AttributeDefinition(name = "Hard commit every N batches")
    int hardCommitEveryNBatches() default 10;

    @AttributeDefinition(name = "Hard commit on successful completion")
    boolean hardCommitOnSuccess() default true;

    @AttributeDefinition(name = "Batch throttle (ms)", description = "Sleep between successful batch flushes")
    long batchThrottleMs() default 300L;

    @AttributeDefinition(name = "Resolver refresh every N query pages")
    int resolverRefreshEveryNBatches() default 20;
}
