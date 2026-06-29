package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class SiteApplicationConfigServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @InjectMocks
    private SiteApplicationConfigServiceImpl siteApplicationConfigService;

    @Mock
    private ResolverUtil resolverUtil;

    @BeforeEach
    void setup() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void getSiteMappings_loadsMappingsFromJcr() {
        context.create().resource(
                SiteApplicationConfigServiceImpl.CONFIG_PATH,
                "siteMappings",
                "[{\"siteRootPath\":\"/content/wknd\",\"enabled\":true}]");

        siteApplicationConfigService.refreshSiteMappings();

        assertEquals(1, siteApplicationConfigService.getSiteMappings().size());
        assertEquals("/content/wknd", siteApplicationConfigService.getSiteMappings().get(0).getSiteRootPath());
        assertTrue(siteApplicationConfigService.getSiteMappings().get(0).isEnabled());
    }

    @Test
    void getSiteMappings_returnsEmptyListWhenConfigMissing() {
        siteApplicationConfigService.refreshSiteMappings();

        assertTrue(siteApplicationConfigService.getSiteMappings().isEmpty());
    }

    @Test
    void getSiteMappings_returnsEmptyListForBlankJson() {
        context.create().resource(SiteApplicationConfigServiceImpl.CONFIG_PATH, "siteMappings", "   ");

        siteApplicationConfigService.refreshSiteMappings();

        assertTrue(siteApplicationConfigService.getSiteMappings().isEmpty());
    }
}
