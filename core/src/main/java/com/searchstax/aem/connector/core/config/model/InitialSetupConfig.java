package com.searchstax.aem.connector.core.config.model;

import java.util.Arrays;

public class InitialSetupConfig {

    private boolean enableConnector;
    private String[] rootPaths;
    private String[] excludePaths;
    private String[] allowedFiles;

    public boolean isEnableConnector() {
        return enableConnector;
    }

    public void setEnableConnector(boolean enableConnector) {
        this.enableConnector = enableConnector;
    }

    public String[] getRootPaths() {
        return rootPaths != null
                ? Arrays.copyOf(
                rootPaths,
                rootPaths.length)
                : null;
    }

    public void setRootPaths(String[] rootPaths) {
        this.rootPaths =
                rootPaths != null 
                        ? Arrays.copyOf(
                        rootPaths,
                        rootPaths.length)
                        : null;
    }

    public String[] getExcludePaths() {

        return excludePaths != null
                ? Arrays.copyOf(
                excludePaths,
                excludePaths.length)
                : null;
    }

    public void setExcludePaths(String[] excludePaths) {

        this.excludePaths =
                excludePaths != null
                        ? Arrays.copyOf(
                        excludePaths,
                        excludePaths.length)
                        : null;
    }

    public String[] getAllowedFiles() {

        return allowedFiles != null
                ? Arrays.copyOf(
                allowedFiles,
                allowedFiles.length)
                : null;
    }

    public void setAllowedFiles(String[] allowedFiles) {
        this.allowedFiles =
                allowedFiles != null
                        ? Arrays.copyOf(
                        allowedFiles,
                        allowedFiles.length)
                        : null;
    }
}