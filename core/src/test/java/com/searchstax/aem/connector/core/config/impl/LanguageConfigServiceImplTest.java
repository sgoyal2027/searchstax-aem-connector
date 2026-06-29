package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageConfigServiceImplTest {

    private LanguageConfigServiceImpl languageConfigService;

    @BeforeEach
    void setUp() throws Exception {
        languageConfigService = new LanguageConfigServiceImpl();
        setCachedMappings(languageConfigService, List.of(
                enabledMapping("en", null, "en", true),
                enabledMapping("custom", "en-gb", "gb", true),
                enabledMapping("fr", null, "fr", false)));
    }

    @Test
    void mapToSearchStaxLanguage_returnsMappedValue() {
        assertEquals("gb", languageConfigService.mapToSearchStaxLanguage("en-gb"));
    }

    @Test
    void mapToSearchStaxLanguage_returnsInputWhenNoMatch() {
        assertEquals("de", languageConfigService.mapToSearchStaxLanguage("de"));
    }

    @Test
    void mapToSearchStaxLanguage_ignoresDisabledMappings() {
        assertEquals("fr", languageConfigService.mapToSearchStaxLanguage("fr"));
    }

    @Test
    void resolveLanguageFromPath_returnsMappedLanguageFromPathSegment() {
        assertEquals("en", languageConfigService.resolveLanguageFromPath("/content/dam/wknd/en/report.pdf"));
    }

    @Test
    void resolveLanguageFromPath_returnsDefaultWhenNoLanguageInPath() {
        assertEquals("en", languageConfigService.resolveLanguageFromPath("/content/dam/wknd/documents/report.pdf"));
    }

    @Test
    void resolveLanguageFromPath_returnsCustomMappedLanguage() {
        assertEquals("gb", languageConfigService.resolveLanguageFromPath("/content/dam/wknd/en-gb/report.pdf"));
    }

    @Test
    void resolveLanguageFromPath_returnsDefaultForBlankPath() {
        assertEquals("en", languageConfigService.resolveLanguageFromPath(" "));
    }

    @Test
    void resolveLanguageFromPath_resolvesFullLanguageNameInPath() {
        assertEquals("en", languageConfigService.resolveLanguageFromPath("/content/dam/wknd/english/report.pdf"));
    }

    private static LanguageMappingConfig enabledMapping(
            final String aemLanguage,
            final String customAemLanguage,
            final String searchStaxLanguage,
            final boolean enabled) {
        final LanguageMappingConfig config = new LanguageMappingConfig();
        config.setAemLanguage(aemLanguage);
        config.setCustomAemLanguage(customAemLanguage);
        config.setSearchStaxLanguage(searchStaxLanguage);
        config.setEnabledLanguageMapping(enabled);
        return config;
    }

    private static void setCachedMappings(
            final LanguageConfigServiceImpl service,
            final List<LanguageMappingConfig> mappings) throws Exception {
        final Field field = LanguageConfigServiceImpl.class.getDeclaredField("cachedMappings");
        field.setAccessible(true);
        field.set(service, mappings);
    }
}
