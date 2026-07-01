package com.searchstax.aem.connector.core.config.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FullIndexConfig {

    private boolean enableConnector;

    private String[] rootPaths;
    
    private List<FullIndexIncludePathConfig> includePaths;
    
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

    public List<FullIndexIncludePathConfig> getIncludePaths() {
        return includePaths != null
            ? new ArrayList<>(includePaths)
            : new ArrayList<>();
    }

    public void setIncludePaths(
            List<FullIndexIncludePathConfig> includePaths) {

        this.includePaths = includePaths != null
            ? new ArrayList<>(includePaths)
            : new ArrayList<>();
    }

    public String[] getExcludePaths() {

        return excludePaths != null
                ? Arrays.copyOf(
                excludePaths,
                excludePaths.length)
                : new String[0];
    }

    public void setExcludePaths(String[] excludePaths) {

        this.excludePaths =
                excludePaths != null
                        ? Arrays.copyOf(
                        excludePaths,
                        excludePaths.length)
                        : new String[0];
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