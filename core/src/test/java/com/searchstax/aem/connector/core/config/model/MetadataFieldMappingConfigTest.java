package com.searchstax.aem.connector.core.config.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataFieldMappingConfigTest {

    @Test
    void testDefaultValues() {

        MetadataFieldMappingConfig config = new MetadataFieldMappingConfig();

        assertNull(config.getAemField());
        assertNull(config.getCustomProperty());
        assertNull(config.getSearchStaxField());
        assertNull(config.getSearchStaxFieldType());

        assertFalse(config.isEnabled());
        assertFalse(config.isMandatory());
    }

    @Test
    void testGettersAndSetters() {

        MetadataFieldMappingConfig config = new MetadataFieldMappingConfig();

        config.setAemField("dc:title");
        config.setCustomProperty("custom:title");
        config.setSearchStaxField("title_txt");
        config.setSearchStaxFieldType("text");
        config.setEnabled(true);
        config.setMandatory(true);

        assertEquals("dc:title", config.getAemField());
        assertEquals("custom:title", config.getCustomProperty());
        assertEquals("title_txt", config.getSearchStaxField());
        assertEquals("text", config.getSearchStaxFieldType());

        assertTrue(config.isEnabled());
        assertTrue(config.isMandatory());
    }

    @Test
    void testBooleanFlagsFalse() {

        MetadataFieldMappingConfig config = new MetadataFieldMappingConfig();

        config.setEnabled(false);
        config.setMandatory(false);

        assertFalse(config.isEnabled());
        assertFalse(config.isMandatory());
    }
}