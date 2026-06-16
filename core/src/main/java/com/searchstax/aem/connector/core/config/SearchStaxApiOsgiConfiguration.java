package com.searchstax.aem.connector.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * API-related connector settings. Package defaults come from AEM run-mode {@code cfg.json} files;
 * the API Configuration wizard may apply a runtime Configuration Admin override on Save.
 */
@ObjectClassDefinition(
        name = "SearchStax Connector — API configuration",
        description = "Endpoints and tokens for the SearchStax connector API wizard.")
public @interface SearchStaxApiOsgiConfiguration {

    @AttributeDefinition(name = "Endpoint URL")
    String endpointUrl() default "";

    @AttributeDefinition(name = "API token")
    String apiToken() default "";

    @AttributeDefinition(name = "Select endpoint")
    String selectEndpoint() default "";

    @AttributeDefinition(name = "Select token")
    String selectToken() default "";

    @AttributeDefinition(name = "Update endpoint")
    String updateEndpoint() default "";

    @AttributeDefinition(name = "Update token")
    String updateToken() default "";

    @AttributeDefinition(name = "Auto-suggest API")
    String autoSuggestApi() default "";

    @AttributeDefinition(name = "Related searches endpoint")
    String relatedSearchesEndpoint() default "";

    @AttributeDefinition(name = "Popular searches endpoint")
    String popularSearchesEndpoint() default "";

    @AttributeDefinition(name = "Discovery API key")
    String discoveryApiKey() default "";

    @AttributeDefinition(name = "Analytics tracking URL")
    String analyticsTrackingUrl() default "";

    @AttributeDefinition(name = "Analytics tracking key")
    String analyticsTrackingKey() default "";

    @AttributeDefinition(name = "Analytics reporting URL")
    String analyticsReportingUrl() default "";

    @AttributeDefinition(name = "Analytics reporting API key")
    String analyticsReportingApiKey() default "";

    @AttributeDefinition(name = "Forward geocoding endpoint")
    String forwardGeocodingEndpoint() default "";

    @AttributeDefinition(name = "Reverse geocoding endpoint")
    String reverseGeocodingEndpoint() default "";
}
