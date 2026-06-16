package com.searchstax.aem.connector.core.config.model;

public class FullIndexIncludePathConfig {

    private String path;

    private boolean includeChildPath;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isIncludeChildPath() {
        return includeChildPath;
    }

    public void setIncludeChildPath(
            boolean includeChildPath) {

        this.includeChildPath =
                includeChildPath;
    }
    
}
