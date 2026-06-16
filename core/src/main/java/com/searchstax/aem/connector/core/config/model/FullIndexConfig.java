package com.searchstax.aem.connector.core.config.model;

import java.util.Arrays;
import java.util.List;

public class FullIndexConfig {

    private String rootPath;
    
    private List<FullIndexIncludePathConfig> includePaths;
    
    private String[] excludePaths;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public List<FullIndexIncludePathConfig> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(
            List<FullIndexIncludePathConfig> includePaths) {

        this.includePaths = includePaths;
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

}