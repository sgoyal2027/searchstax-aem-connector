package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiConfigServiceImplTest {

    @InjectMocks
    private ApiConfigServiceImpl service;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;

    @BeforeEach
    void setup() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
    }

    @Test
    void testGetConfigurationSuccess() throws Exception {

        when(resourceResolver.getResource(ApiConfigServiceImpl.CONFIG_PATH))
                .thenReturn(resource);

        when(resource.getValueMap()).thenReturn(valueMap);

        when(valueMap.get("endpointUrl", "")).thenReturn("endpoint");
        when(valueMap.get("apiToken", "")).thenReturn("token");
        when(valueMap.get("selectEndpoint", "")).thenReturn("select");
        when(valueMap.get("selectToken", "")).thenReturn("selectToken");
        when(valueMap.get("updateEndpoint", "")).thenReturn("update");
        when(valueMap.get("updateToken", "")).thenReturn("updateToken");
        when(valueMap.get("autoSuggestApi", "")).thenReturn("autosuggest");
        when(valueMap.get("relatedSearchesEndpoint", "")).thenReturn("related");
        when(valueMap.get("popularSearchesEndpoint", "")).thenReturn("popular");
        when(valueMap.get("discoveryApiKey", "")).thenReturn("discovery");
        when(valueMap.get("analyticsTrackingUrl", "")).thenReturn("trackingUrl");
        when(valueMap.get("analyticsTrackingKey", "")).thenReturn("trackingKey");
        when(valueMap.get("analyticsReportingUrl", "")).thenReturn("reportingUrl");
        when(valueMap.get("analyticsReportingApiKey", "")).thenReturn("reportingKey");
        when(valueMap.get("forwardGeocodingEndpoint", "")).thenReturn("forward");
        when(valueMap.get("reverseGeocodingEndpoint", "")).thenReturn("reverse");

        ApiConfig config = service.getConfiguration();

        assertEquals("endpoint", config.getEndpointUrl());
        assertEquals("token", config.getApiToken());
        assertEquals("select", config.getSelectEndpoint());
        assertEquals("selectToken", config.getSelectToken());
        assertEquals("update", config.getUpdateEndpoint());
        assertEquals("updateToken", config.getUpdateToken());
        assertEquals("autosuggest", config.getAutoSuggestApi());
        assertEquals("related", config.getRelatedSearchesEndpoint());
        assertEquals("popular", config.getPopularSearchesEndpoint());
        assertEquals("discovery", config.getDiscoveryApiKey());
        assertEquals("trackingUrl", config.getAnalyticsTrackingUrl());
        assertEquals("trackingKey", config.getAnalyticsTrackingKey());
        assertEquals("reportingUrl", config.getAnalyticsReportingUrl());
        assertEquals("reportingKey", config.getAnalyticsReportingApiKey());
        assertEquals("forward", config.getForwardGeocodingEndpoint());
        assertEquals("reverse", config.getReverseGeocodingEndpoint());

        verify(resourceResolver).close();
    }

    @Test
    void testConfigurationNotFound() throws Exception {

        when(resourceResolver.getResource(ApiConfigServiceImpl.CONFIG_PATH))
                .thenReturn(null);

        ApiConfig config = service.getConfiguration();

        assertNotNull(config);

        assertNull(config.getEndpointUrl());
        assertNull(config.getApiToken());

        verify(resourceResolver).close();
    }

    @Test
    void testLoginException() throws Exception {

        when(resolverUtil.getServiceResolver())
                .thenThrow(new LoginException("login failed"));

        ApiConfig config = service.getConfiguration();

        assertNotNull(config);

        assertNull(config.getEndpointUrl());
        assertNull(config.getApiToken());
    }
}