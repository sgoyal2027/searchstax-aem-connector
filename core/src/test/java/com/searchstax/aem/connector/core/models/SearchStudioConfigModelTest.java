package com.searchstax.aem.connector.core.models;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchStudioConfigModelTest {

    @InjectMocks
    private SearchStudioConfigModel model;

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    @BeforeEach
    void init() throws Exception {
        Method method = SearchStudioConfigModel.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(model);
    }

    @Test
    void testGetters() throws Exception {

        ApiConfig config = new ApiConfig();
        config.setSelectEndpoint("https://search-url");
        config.setAutoSuggestApi("https://suggest-url");
        config.setSelectToken("select-token");
        config.setAnalyticsTrackingKey("tracking-key");
        config.setAnalyticsTrackingUrl("https://analytics-url");
        config.setForwardGeocodingEndpoint("forward-url");
        config.setReverseGeocodingEndpoint("reverse-url");
        config.setDiscoveryApiKey("discovery-key");

        when(apiConfigService.getConfiguration()).thenReturn(config);

        when(protectedValueCodec.unprotectIfNeeded("select-token"))
                .thenReturn("decoded-select-token");

        when(protectedValueCodec.unprotectIfNeeded("tracking-key"))
                .thenReturn("decoded-tracking-key");

        when(protectedValueCodec.unprotectIfNeeded("discovery-key"))
                .thenReturn("decoded-discovery-key");

        Method method = SearchStudioConfigModel.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(model);

        assertEquals("https://search-url", model.getSearchURL());
        assertEquals("https://suggest-url", model.getSuggesterURL());
        assertEquals("decoded-select-token", model.getSearchAuth());
        assertEquals("decoded-tracking-key", model.getTrackApiKey());
        assertEquals("https://analytics-url", model.getAnalyticsBaseUrl());
        assertEquals("forward-url", model.getForwardGeocodingEndpoint());
        assertEquals("reverse-url", model.getReverseGeocodingEndpoint());
        assertEquals("decoded-discovery-key", model.getDiscoveryApiKey());

        verify(protectedValueCodec).unprotectIfNeeded("select-token");
        verify(protectedValueCodec).unprotectIfNeeded("tracking-key");
        verify(protectedValueCodec).unprotectIfNeeded("discovery-key");
    }

    @Test
    void testNullConfiguration() throws Exception {

        when(apiConfigService.getConfiguration()).thenReturn(null);

        Method method = SearchStudioConfigModel.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(model);

        assertEquals("", model.getSearchURL());
        assertEquals("", model.getSuggesterURL());
        assertEquals("", model.getSearchAuth());
        assertEquals("", model.getTrackApiKey());
        assertEquals("", model.getAnalyticsBaseUrl());
        assertEquals("", model.getForwardGeocodingEndpoint());
        assertEquals("", model.getReverseGeocodingEndpoint());
        assertEquals("", model.getDiscoveryApiKey());

        verifyNoInteractions(protectedValueCodec);
    }
}