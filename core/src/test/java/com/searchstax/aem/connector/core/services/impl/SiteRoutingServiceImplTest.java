package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.SiteApplicationConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.config.model.SiteApplicationMappingConfig;
import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteRoutingServiceImplTest {

    @Mock
    private SiteApplicationConfigService siteApplicationConfigService;

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
    void resolve_usesLongestMatchingEnabledSiteRoot() {
        when(siteApplicationConfigService.getSiteMappings()).thenReturn(List.of(
                mapping("/content/wknd", "https://wknd.example/update", "wknd-token", "wknd-profile", true),
                mapping("/content/wknd/us", "https://wknd-us.example/update", "wknd-us-token", "us-profile", true)));

        final SiteRoutingResult result =
                siteRoutingService.resolve("/content/wknd/us/en/adventures");

        assertEquals("https://wknd-us.example/update", result.getUpdateEndpoint());
        assertEquals("wknd-us-token", result.getUpdateToken());
        assertEquals("us-profile", result.getSearchProfile());
    }

    @Test
    void resolve_fallsBackToGlobalWhenNoMatch() {
        when(siteApplicationConfigService.getSiteMappings()).thenReturn(List.of(
                mapping("/content/other", "https://other.example/update", "other-token", null, true)));

        final SiteRoutingResult result = siteRoutingService.resolve("/content/wknd/en/page");

        assertEquals("https://global.example/update", result.getUpdateEndpoint());
        assertEquals("global-token", result.getUpdateToken());
    }

    @Test
    void resolve_ignoresDisabledMappings() {
        when(siteApplicationConfigService.getSiteMappings()).thenReturn(List.of(
                mapping("/content/wknd", "https://wknd.example/update", "wknd-token", null, false)));

        final SiteRoutingResult result = siteRoutingService.resolve("/content/wknd/en/page");

        assertEquals("https://global.example/update", result.getUpdateEndpoint());
    }

    private static SiteApplicationMappingConfig mapping(
            final String root,
            final String endpoint,
            final String token,
            final String profile,
            final boolean enabled) {
        final SiteApplicationMappingConfig config = new SiteApplicationMappingConfig();
        config.setSiteRootPath(root);
        config.setUpdateEndpoint(endpoint);
        config.setUpdateToken(token);
        config.setSearchProfile(profile);
        config.setEnabled(enabled);
        return config;
    }
}
