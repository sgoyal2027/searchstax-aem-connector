package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteRoutingServiceImplTest {

    @Mock
    private ApiConfigService apiConfigService;

    @InjectMocks
    private SiteRoutingServiceImpl siteRoutingService;

    private ApiConfig globalConfig;

    @BeforeEach
    void setUp() {
        globalConfig = new ApiConfig();
        globalConfig.setUpdateEndpoint("https://global.example/update");
        globalConfig.setUpdateToken("global-token");
        when(apiConfigService.getConfiguration()).thenReturn(globalConfig);
    }

    @Test
    void resolve_usesGlobalApiConfiguration() {
        final SiteRoutingResult result = siteRoutingService.resolve("/content/wknd/en/page");

        assertEquals("https://global.example/update", result.getUpdateEndpoint());
        assertEquals("global-token", result.getUpdateToken());
        assertNull(result.getSearchProfile());
        assertNull(result.getMatchedSiteRoot());
    }
}
