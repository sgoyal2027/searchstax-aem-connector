package com.searchstax.aem.connector.core.config.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiConfigTest {

    @Test
    void testDefaultValues() {
        ApiConfig config = new ApiConfig();

        assertNull(config.getEndpointUrl());
        assertNull(config.getApiToken());
        assertNull(config.getSelectEndpoint());
        assertNull(config.getSelectToken());
        assertNull(config.getUpdateEndpoint());
        assertNull(config.getUpdateToken());
        assertNull(config.getAutoSuggestApi());
        assertNull(config.getRelatedSearchesEndpoint());
        assertNull(config.getPopularSearchesEndpoint());
        assertNull(config.getDiscoveryApiKey());
        assertNull(config.getAnalyticsTrackingUrl());
        assertNull(config.getAnalyticsTrackingKey());
        assertNull(config.getAnalyticsReportingUrl());
        assertNull(config.getAnalyticsReportingApiKey());
        assertNull(config.getForwardGeocodingEndpoint());
        assertNull(config.getReverseGeocodingEndpoint());
    }

    @Test
    void testGettersAndSetters() {

        ApiConfig config = new ApiConfig();

        config.setEndpointUrl("https://api.searchstax.com");
        config.setApiToken("api-token");
        config.setSelectEndpoint("https://api.searchstax.com/select");
        config.setSelectToken("select-token");
        config.setUpdateEndpoint("https://api.searchstax.com/update");
        config.setUpdateToken("update-token");
        config.setAutoSuggestApi("https://api.searchstax.com/autosuggest");
        config.setRelatedSearchesEndpoint("https://api.searchstax.com/related-searches");
        config.setPopularSearchesEndpoint("https://api.searchstax.com/popular-searches");
        config.setDiscoveryApiKey("discovery-key");
        config.setAnalyticsTrackingUrl("https://analytics.searchstax.com/track");
        config.setAnalyticsTrackingKey("tracking-key");
        config.setAnalyticsReportingUrl("https://analytics.searchstax.com/report");
        config.setAnalyticsReportingApiKey("reporting-key");
        config.setForwardGeocodingEndpoint("https://api.searchstax.com/geocode/forward");
        config.setReverseGeocodingEndpoint("https://api.searchstax.com/geocode/reverse");

        assertEquals("https://api.searchstax.com", config.getEndpointUrl());
        assertEquals("api-token", config.getApiToken());
        assertEquals("https://api.searchstax.com/select", config.getSelectEndpoint());
        assertEquals("select-token", config.getSelectToken());
        assertEquals("https://api.searchstax.com/update", config.getUpdateEndpoint());
        assertEquals("update-token", config.getUpdateToken());
        assertEquals("https://api.searchstax.com/autosuggest", config.getAutoSuggestApi());
        assertEquals("https://api.searchstax.com/related-searches", config.getRelatedSearchesEndpoint());
        assertEquals("https://api.searchstax.com/popular-searches", config.getPopularSearchesEndpoint());
        assertEquals("discovery-key", config.getDiscoveryApiKey());
        assertEquals("https://analytics.searchstax.com/track", config.getAnalyticsTrackingUrl());
        assertEquals("tracking-key", config.getAnalyticsTrackingKey());
        assertEquals("https://analytics.searchstax.com/report", config.getAnalyticsReportingUrl());
        assertEquals("reporting-key", config.getAnalyticsReportingApiKey());
        assertEquals("https://api.searchstax.com/geocode/forward", config.getForwardGeocodingEndpoint());
        assertEquals("https://api.searchstax.com/geocode/reverse", config.getReverseGeocodingEndpoint());
    }
}