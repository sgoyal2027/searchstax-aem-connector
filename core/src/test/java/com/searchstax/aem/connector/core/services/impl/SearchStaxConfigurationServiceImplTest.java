package com.searchstax.aem.connector.core.services.impl;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxConfigurationServiceImplTest {

    @InjectMocks
    private SearchStaxConfigurationServiceImpl configurationService;

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private CryptoSupport cryptoSupport;

    private ApiConfig apiConfig;

    @BeforeEach
    void setup() {
        apiConfig = new ApiConfig();
        apiConfig.setEndpointUrl("https://api.searchstax.com");
        apiConfig.setApiToken("{protected-api}");
        apiConfig.setSelectEndpoint("/select");
        apiConfig.setSelectToken("{protected-select}");
        apiConfig.setUpdateEndpoint("/update");
        apiConfig.setUpdateToken("{protected-update}");
        apiConfig.setDiscoveryApiKey("{protected-discovery}");
        apiConfig.setAutoSuggestApi("/suggest");
        when(apiConfigService.getConfiguration()).thenReturn(apiConfig);
    }

    @Test
    void getEndpointUrl_returnsConfiguredValue() {
        assertEquals("https://api.searchstax.com", configurationService.getEndpointUrl());
    }

    @Test
    void getApiToken_decryptsProtectedValue() throws Exception {
        when(cryptoSupport.isProtected("{protected-api}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected-api}")).thenReturn("api-plain");

        assertEquals("api-plain", configurationService.getApiToken());
    }

    @Test
    void getSelectToken_decryptsProtectedValue() throws Exception {
        when(cryptoSupport.isProtected("{protected-select}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected-select}")).thenReturn("select-plain");

        assertEquals("select-plain", configurationService.getSelectToken());
    }

    @Test
    void getUpdateToken_returnsRawValueWhenCryptoFails() throws Exception {
        when(cryptoSupport.isProtected("{protected-update}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected-update}")).thenThrow(new CryptoException("failed"));

        assertEquals("{protected-update}", configurationService.getUpdateToken());
    }

    @Test
    void getDiscoveryApiKey_returnsPlainValueWhenNotProtected() {
        apiConfig.setDiscoveryApiKey("plain-discovery");

        assertEquals("plain-discovery", configurationService.getDiscoveryApiKey());
    }

    @Test
    void getAutoSuggestApi_returnsConfiguredValue() {
        assertEquals("/suggest", configurationService.getAutoSuggestApi());
    }

    @Test
    void getSelectAndUpdateEndpoints_returnConfiguredValues() {
        assertEquals("/select", configurationService.getSelectEndpoint());
        assertEquals("/update", configurationService.getUpdateEndpoint());
    }

    @Test
    void getRelatedAndPopularSearchEndpoints_returnConfiguredValues() {
        apiConfig.setRelatedSearchesEndpoint("/related");
        apiConfig.setPopularSearchesEndpoint("/popular");

        assertEquals("/related", configurationService.getRelatedSearchesEndpoint());
        assertEquals("/popular", configurationService.getPopularSearchesEndpoint());
    }

    @Test
    void getAnalyticsUrls_returnConfiguredValues() {
        apiConfig.setAnalyticsTrackingUrl("https://track.example.com");
        apiConfig.setAnalyticsReportingUrl("https://report.example.com");

        assertEquals("https://track.example.com", configurationService.getAnalyticsTrackingUrl());
        assertEquals("https://report.example.com", configurationService.getAnalyticsReportingUrl());
    }

    @Test
    void getAnalyticsKeys_decryptProtectedValues() throws Exception {
        apiConfig.setAnalyticsTrackingKey("{protected-tracking}");
        apiConfig.setAnalyticsReportingApiKey("{protected-reporting}");
        when(cryptoSupport.isProtected("{protected-tracking}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected-tracking}")).thenReturn("tracking-plain");
        when(cryptoSupport.isProtected("{protected-reporting}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected-reporting}")).thenReturn("reporting-plain");

        assertEquals("tracking-plain", configurationService.getAnalyticsTrackingKey());
        assertEquals("reporting-plain", configurationService.getAnalyticsReportingApiKey());
    }

    @Test
    void getGeocodingEndpoints_returnConfiguredValues() {
        apiConfig.setForwardGeocodingEndpoint("/geo/forward");
        apiConfig.setReverseGeocodingEndpoint("/geo/reverse");

        assertEquals("/geo/forward", configurationService.getForwardGeocodingEndpoint());
        assertEquals("/geo/reverse", configurationService.getReverseGeocodingEndpoint());
    }
}
