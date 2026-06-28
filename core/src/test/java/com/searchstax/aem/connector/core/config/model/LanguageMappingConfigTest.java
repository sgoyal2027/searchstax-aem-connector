package com.searchstax.aem.connector.core.config.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageMappingConfigTest {

    @Test
    void testDefaultValues() {

        LanguageMappingConfig config = new LanguageMappingConfig();

        assertNull(config.getAemLanguage());
        assertNull(config.getCustomAemLanguage());
        assertNull(config.getSearchStaxLanguage());
        assertFalse(config.isEnabledLanguageMapping());
    }

    @Test
    void testGettersAndSetters() {

        LanguageMappingConfig config = new LanguageMappingConfig();

        config.setAemLanguage("fr");
        config.setCustomAemLanguage("fr_CA");
        config.setSearchStaxLanguage("fr");
        config.setEnabledLanguageMapping(true);

        assertEquals("fr", config.getAemLanguage());
        assertEquals("fr_CA", config.getCustomAemLanguage());
        assertEquals("fr", config.getSearchStaxLanguage());
        assertTrue(config.isEnabledLanguageMapping());
    }

    @Test
    void testEnabledLanguageMappingFalse() {

        LanguageMappingConfig config = new LanguageMappingConfig();

        config.setEnabledLanguageMapping(false);

        assertFalse(config.isEnabledLanguageMapping());
    }

    @Test
    void testDefaultMappings() {

        List<LanguageMappingConfig> mappings =
                LanguageMappingConfig.defaultMappings();

        assertNotNull(mappings);
        assertEquals(1, mappings.size());

        LanguageMappingConfig mapping = mappings.get(0);

        assertEquals("en", mapping.getAemLanguage());
        assertEquals("en", mapping.getSearchStaxLanguage());
        assertTrue(mapping.isEnabledLanguageMapping());
        assertNull(mapping.getCustomAemLanguage());
    }

    @Test
    void testDefaultMappingsIsImmutable() {

        List<LanguageMappingConfig> mappings =
                LanguageMappingConfig.defaultMappings();

        assertThrows(
                UnsupportedOperationException.class,
                () -> mappings.add(new LanguageMappingConfig()));
    }

    @Test
    void testIsEmptyMappingsJson() {
        assertTrue(LanguageMappingConfig.isEmptyMappingsJson(null));
        assertTrue(LanguageMappingConfig.isEmptyMappingsJson(""));
        assertTrue(LanguageMappingConfig.isEmptyMappingsJson("   "));
        assertTrue(LanguageMappingConfig.isEmptyMappingsJson("[]"));
        assertFalse(LanguageMappingConfig.isEmptyMappingsJson("[{\"aemLanguage\":\"en\"}]"));
    }
}