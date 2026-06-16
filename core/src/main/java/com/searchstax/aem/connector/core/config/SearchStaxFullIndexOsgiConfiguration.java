package com.searchstax.aem.connector.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Full-index path allowlists (one named instance per environment: {@code pid~dev}, etc.).
 * Runtime tuning uses {@link SearchStaxFullIndexRuntimeConfiguration}; path config for the
 * Full Index wizard is persisted under {@code /conf/searchstaxconnector/settings/fullindexsetupconfig}.
 */
@ObjectClassDefinition(
        name = "SearchStax Connector — Full index path configuration",
        description = "Include and exclude paths for legacy OSGi-backed full index wizard bindings.")
public @interface SearchStaxFullIndexOsgiConfiguration {

    @AttributeDefinition(name = "Include paths")
    String[] includePaths() default {};

    @AttributeDefinition(name = "Exclude paths")
    String[] excludePaths() default {};
}
