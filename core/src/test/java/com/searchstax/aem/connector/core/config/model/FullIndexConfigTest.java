package com.searchstax.aem.connector.core.config.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FullIndexConfigTest {

    @Test
    void testDefaultValues() {

        FullIndexConfig config = new FullIndexConfig();

        assertFalse(config.isEnableConnector());
        assertNull(config.getRootPaths());
        assertTrue(config.getIncludePaths().isEmpty());
        assertNotNull(config.getExcludePaths());
        assertEquals(0, config.getExcludePaths().length);
        assertNull(config.getAllowedFiles());
    }

    @Test
    void testGettersAndSetters() {

        FullIndexConfig config = new FullIndexConfig();

        String[] rootPaths = {"/content/site1", "/content/site2"};

        FullIndexIncludePathConfig includePath = new FullIndexIncludePathConfig();
        config.setIncludePaths(Collections.singletonList(includePath));

        String[] excludePaths = {
                "/content/exclude1",
                "/content/exclude2"
        };
        String[] allowedFiles = {"pdf", "docx"};

        config.setExcludePaths(excludePaths);
        config.setAllowedFiles(allowedFiles);

        config.setEnableConnector(true);
        config.setRootPaths(rootPaths);
        assertEquals(1, config.getIncludePaths().size());
        assertSame(includePath, config.getIncludePaths().get(0));

        assertArrayEquals(excludePaths, config.getExcludePaths());
        assertArrayEquals(allowedFiles, config.getAllowedFiles());
    }

    @Test
    void testExcludePathsDefensiveCopyOnSetter() {

        FullIndexConfig config = new FullIndexConfig();

        String[] excludePaths = {
                "/content/exclude1"
        };

        config.setExcludePaths(excludePaths);

        // Modify original array
        excludePaths[0] = "/modified";

        assertEquals(
                "/content/exclude1",
                config.getExcludePaths()[0]);
    }

    @Test
    void testExcludePathsDefensiveCopyOnGetter() {

        FullIndexConfig config = new FullIndexConfig();

        config.setExcludePaths(new String[]{
                "/content/exclude1"
        });

        String[] returned = config.getExcludePaths();

        // Modify returned array
        returned[0] = "/modified";

        assertEquals(
                "/content/exclude1",
                config.getExcludePaths()[0]);
    }

    @Test
    void testSetExcludePathsWithNull() {

        FullIndexConfig config = new FullIndexConfig();

        config.setExcludePaths(null);

        assertNotNull(config.getExcludePaths());
        assertEquals(0, config.getExcludePaths().length);
    }
}