package com.searchstax.aem.connector.core.services;

/**
 * Provides read access to all SearchStax endpoint settings persisted in OSGi configuration.
 * A single instance of this service is the authoritative source; all indexing components
 * (full-index and incremental) must read settings exclusively through this interface.
 */
public interface SearchStaxConfigurationService {

    String getEndpointUrl();

    String getApiToken();

    String getSelectEndpoint();

    String getSelectToken();

    String getUpdateEndpoint();

    String getUpdateToken();

    String getAutoSuggestApi();

    String getRelatedSearchesEndpoint();

    String getPopularSearchesEndpoint();

    String getDiscoveryApiKey();

    String getAnalyticsTrackingUrl();

    String getAnalyticsTrackingKey();

    String getAnalyticsReportingUrl();

    String getAnalyticsReportingApiKey();

    String getForwardGeocodingEndpoint();

    String getReverseGeocodingEndpoint();
}
