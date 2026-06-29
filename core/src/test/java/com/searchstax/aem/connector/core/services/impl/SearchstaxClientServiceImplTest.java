package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;
import com.searchstax.aem.connector.core.dto.SearchStaxUpdateOptions;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.SiteRoutingService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchstaxClientServiceImplTest {

    @InjectMocks
    private SearchstaxClientServiceImpl clientService;

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    @Mock
    private SiteRoutingService siteRoutingService;

    private ApiConfig apiConfig;

    @BeforeEach
    void setup() {
        apiConfig = new ApiConfig();
        when(apiConfigService.getConfiguration()).thenReturn(apiConfig);
    }

    @Test
    void buildRequestUrl_incrementalHardCommit() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update", SearchStaxUpdateOptions.incrementalDefault());

        assertTrue(url.contains("commit=true"));
    }

    @Test
    void buildRequestUrl_fullIndexCommitWithinAndHardCommit() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update",
                SearchStaxUpdateOptions.fullIndexBatch(true, 120000L));

        assertTrue(url.contains("commitWithin=120000"));
        assertTrue(url.contains("commit=true"));
    }

    @Test
    void buildRequestUrl_includesSearchProfileModelParam() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update",
                SearchStaxUpdateOptions.routed(true, null, null, null, "my-profile"));

        assertTrue(url.contains("Model=my-profile"));
    }

    @Test
    void postUpdate_returnsTransportErrorWhenEndpointMissing() {
        apiConfig.setUpdateEndpoint("");
        apiConfig.setUpdateToken("token");
        when(protectedValueCodec.unprotectIfNeeded("token")).thenReturn("token");

        final ApiResponse response = clientService.postUpdate("{}", SearchStaxUpdateOptions.incrementalDefault());

        assertEquals(SearchstaxClientServiceImpl.TRANSPORT_ERROR_STATUS, response.getStatusCode());
        assertTrue(response.getResponseBody().contains("update endpoint is not configured"));
    }

    @Test
    void postUpdate_returnsTransportErrorWhenTokenMissing() {
        apiConfig.setUpdateEndpoint("https://search.example/update");
        apiConfig.setUpdateToken("");
        when(protectedValueCodec.unprotectIfNeeded("")).thenReturn("");

        final ApiResponse response = clientService.postUpdate("{}", SearchStaxUpdateOptions.incrementalDefault());

        assertEquals(SearchstaxClientServiceImpl.TRANSPORT_ERROR_STATUS, response.getStatusCode());
        assertTrue(response.getResponseBody().contains("update token is not configured"));
    }

    @Test
    void buildRequestUrl_appendsToExistingQueryString() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update?existing=1",
                SearchStaxUpdateOptions.fullIndexBatch(true, 60_000L));

        assertTrue(url.contains("existing=1"));
        assertTrue(url.contains("commitWithin=60000"));
        assertTrue(url.contains("commit=true"));
    }

    @Test
    void buildRequestUrl_skipsDuplicateHardCommitParam() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update?commit=true",
                SearchStaxUpdateOptions.incrementalDefault());

        assertEquals("https://search.example/update?commit=true", url);
    }

    @Test
    void indexDocument_withContentPath_usesRoutedEndpoint() {
        final SiteRoutingResult routing =
                new SiteRoutingResult(
                        "https://site.example/update",
                        "site-token",
                        "site-profile",
                        "/content/wknd");
        when(siteRoutingService.resolve("/content/wknd/en/page")).thenReturn(routing);
        when(protectedValueCodec.unprotectIfNeeded("site-token")).thenReturn("site-token");

        final ApiResponse response =
                clientService.indexDocument("{}", "/content/wknd/en/page");

        assertEquals(SearchstaxClientServiceImpl.TRANSPORT_ERROR_STATUS, response.getStatusCode());
    }

    @Test
    void deleteDocument_withContentPath_usesRoutedEndpoint() {
        final SiteRoutingResult routing =
                new SiteRoutingResult(
                        "https://site.example/update",
                        "site-token",
                        "site-profile",
                        "/content/wknd");
        when(siteRoutingService.resolve("/content/wknd/en/page")).thenReturn(routing);
        when(protectedValueCodec.unprotectIfNeeded("site-token")).thenReturn("site-token");

        final ApiResponse response =
                clientService.deleteDocument("{\"delete\":{}}", "/content/wknd/en/page");

        assertEquals(SearchstaxClientServiceImpl.TRANSPORT_ERROR_STATUS, response.getStatusCode());
    }
}

