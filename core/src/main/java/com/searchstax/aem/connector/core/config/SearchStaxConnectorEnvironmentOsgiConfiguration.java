package com.searchstax.aem.connector.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Singleton OSGi configuration: which logical environment the Touch UI wizard is editing
 * ({@code dev}, {@code prod}, {@code local}). Named API / full-index PIDs use this value as
 * {@code basePid~env}.
 */
@ObjectClassDefinition(
        name = "SearchStax Connector — Active environment (wizard)",
        description = "Selects which named OSGi configuration (dev / prod / local) the author wizard loads and saves.")
public @interface SearchStaxConnectorEnvironmentOsgiConfiguration {

    @AttributeDefinition(
            name = "Active environment",
            description = "One of: dev, prod, local.")
    String activeEnvironment() default "dev";
}
