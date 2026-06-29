package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class LanguageConfigServiceImplLoadTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private LanguageConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new LanguageConfigServiceImpl();
        injectResolverUtil(resolverUtil);
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void activate_loadsMappingsFromConfigurationResource() {
        context.create().resource(
                "/conf/searchstaxconnector/settings/languagemapping",
                "languageMappings",
                "[{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"en\",\"enabledLanguageMapping\":true}]");

        service.activate();

        assertEquals(1, service.getLanguageMappings().size());
        assertEquals("en", service.getLanguageMappings().get(0).getAemLanguage());
    }

    @Test
    void refreshLanguageMappings_reloadsUpdatedJson() {
        context.create().resource(
                "/conf/searchstaxconnector/settings/languagemapping",
                "languageMappings",
                "[{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"en\",\"enabledLanguageMapping\":true}]");

        service.activate();
        assertEquals(1, service.getLanguageMappings().size());

        context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/languagemapping")
                .adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class)
                .put(
                        "languageMappings",
                        "[{\"aemLanguage\":\"fr\",\"searchStaxLanguage\":\"fr\",\"enabledLanguageMapping\":true},"
                                + "{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"en\",\"enabledLanguageMapping\":true}]");

        service.refreshLanguageMappings();

        assertEquals(2, service.getLanguageMappings().size());
        assertEquals("fr", service.getLanguageMappings().get(0).getAemLanguage());
    }

    @Test
    void loadMappings_returnsEmptyListWhenResolverThrows() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("resolver unavailable"));

        service.activate();

        assertTrue(service.getLanguageMappings().isEmpty());
    }

    @Test
    void activate_persistsDefaultMappingsWhenConfigurationMissing() {
        service.activate();

        assertFalse(service.getLanguageMappings().isEmpty());
        assertEquals("en", service.getLanguageMappings().get(0).getAemLanguage());
    }

    private void injectResolverUtil(final ResolverUtil resolverUtil) throws Exception {
        final Field field = LanguageConfigServiceImpl.class.getDeclaredField("resolverUtil");
        field.setAccessible(true);
        field.set(service, resolverUtil);
    }
}
