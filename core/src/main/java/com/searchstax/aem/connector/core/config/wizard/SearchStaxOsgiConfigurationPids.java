package com.searchstax.aem.connector.core.config.wizard;

/**
 * OSGi Configuration Admin PIDs for SearchStax connector settings. Values must match the
 * {@code @ObjectClassDefinition} annotation types under {@code com.searchstax.aem.connector.core.config}
 * (used by {@code ui.config} {@code *.cfg.json} filenames).
 */
public final class SearchStaxOsgiConfigurationPids {

    public static final String API_CONFIGURATION_PID =
            "com.searchstax.aem.connector.core.config.SearchStaxApiOsgiConfiguration";

    public static final String FULL_INDEX_CONFIGURATION_PID =
            "com.searchstax.aem.connector.core.config.SearchStaxFullIndexOsgiConfiguration";

    /** Singleton: active logical environment for the author wizard. */
    public static final String ENVIRONMENT_CONFIGURATION_PID =
            "com.searchstax.aem.connector.core.config.SearchStaxConnectorEnvironmentOsgiConfiguration";

    private SearchStaxOsgiConfigurationPids() {
    }
}
