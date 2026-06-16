package com.searchstax.aem.connector.core.config.model;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "SearchStax Incremental Index Scheduler")
public @interface IncrementalIndexSchedulerConfig {

    @AttributeDefinition(
            name = "Scheduler Expression",
            description = "Cron expression for incremental indexing")
    String scheduler_expression()
            default "0/10 * * * * ?";

    @AttributeDefinition(
            name = "Batch Size",
            description = "Maximum requests processed per batch")
    int batch_size() default 100;
}