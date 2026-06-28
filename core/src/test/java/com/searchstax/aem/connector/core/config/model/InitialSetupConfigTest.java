package com.searchstax.aem.connector.core.config.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InitialSetupConfigTest {

    @Test
    void testDefaultValues() {

        InitialSetupConfig config = new InitialSetupConfig();

        assertFalse(config.isEnableConnector());
        assertNull(config.getRootPaths());
        assertNull(config.getExcludePaths());
        assertNull(config.getAllowedFiles());
    }

    @Test
    void testGettersAndSetters() {

        InitialSetupConfig config = new InitialSetupConfig();

        String[] rootPaths = {"/content/site1", "/content/site2"};
        String[] excludePaths = {"/content/exclude"};
        String[] allowedFiles = {"pdf", "docx"};

        config.setEnableConnector(true);
        config.setRootPaths(rootPaths);
        config.setExcludePaths(excludePaths);
        config.setAllowedFiles(allowedFiles);

        assertTrue(config.isEnableConnector());
        assertArrayEquals(rootPaths, config.getRootPaths());
        assertArrayEquals(excludePaths, config.getExcludePaths());
        assertArrayEquals(allowedFiles, config.getAllowedFiles());
    }

    @Test
    void testRootPathsDefensiveCopy() {

        InitialSetupConfig config = new InitialSetupConfig();

        String[] rootPaths = {"/content/site1"};
        config.setRootPaths(rootPaths);

        rootPaths[0] = "/modified";

        assertEquals("/content/site1", config.getRootPaths()[0]);

        String[] returned = config.getRootPaths();
        returned[0] = "/changed";

        assertEquals("/content/site1", config.getRootPaths()[0]);
    }

    @Test
    void testExcludePathsDefensiveCopy() {

        InitialSetupConfig config = new InitialSetupConfig();

        String[] excludePaths = {"/content/exclude"};
        config.setExcludePaths(excludePaths);

        excludePaths[0] = "/modified";

        assertEquals("/content/exclude", config.getExcludePaths()[0]);

        String[] returned = config.getExcludePaths();
        returned[0] = "/changed";

        assertEquals("/content/exclude", config.getExcludePaths()[0]);
    }

    @Test
    void testAllowedFilesDefensiveCopy() {

        InitialSetupConfig config = new InitialSetupConfig();

        String[] allowedFiles = {"pdf"};
        config.setAllowedFiles(allowedFiles);

        allowedFiles[0] = "docx";

        assertEquals("pdf", config.getAllowedFiles()[0]);

        String[] returned = config.getAllowedFiles();
        returned[0] = "txt";

        assertEquals("pdf", config.getAllowedFiles()[0]);
    }

    @Test
    void testNullArrays() {

        InitialSetupConfig config = new InitialSetupConfig();

        config.setRootPaths(null);
        config.setExcludePaths(null);
        config.setAllowedFiles(null);

        assertNull(config.getRootPaths());
        assertNull(config.getExcludePaths());
        assertNull(config.getAllowedFiles());
    }

    @Test
    void testDisableConnector() {

        InitialSetupConfig config = new InitialSetupConfig();

        config.setEnableConnector(false);

        assertFalse(config.isEnableConnector());
    }
}