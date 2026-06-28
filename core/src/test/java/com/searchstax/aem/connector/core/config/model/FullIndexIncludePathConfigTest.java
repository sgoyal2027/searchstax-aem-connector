package com.searchstax.aem.connector.core.config.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FullIndexIncludePathConfigTest {

    @Test
    void testDefaultValues() {

        FullIndexIncludePathConfig config = new FullIndexIncludePathConfig();

        assertNull(config.getPath());
        assertFalse(config.isIncludeChildPath());
    }

    @Test
    void testGettersAndSetters() {

        FullIndexIncludePathConfig config = new FullIndexIncludePathConfig();

        config.setPath("/content/site");
        config.setIncludeChildPath(true);

        assertEquals("/content/site", config.getPath());
        assertTrue(config.isIncludeChildPath());
    }

    @Test
    void testIncludeChildPathFalse() {

        FullIndexIncludePathConfig config = new FullIndexIncludePathConfig();

        config.setIncludeChildPath(false);

        assertFalse(config.isIncludeChildPath());
    }
}