package com.searchstax.aem.connector.core.config.model;

/**
 * Maps an AEM content root to a SearchStax application update endpoint and optional search profile.
 */
public class SiteApplicationMappingConfig {

    private String siteRootPath;
    private String updateEndpoint;
    private String updateToken;
    private String searchProfile;
    private boolean enabled;

    public String getSiteRootPath() {
        return siteRootPath;
    }

    public void setSiteRootPath(final String siteRootPath) {
        this.siteRootPath = siteRootPath;
    }

    public String getUpdateEndpoint() {
        return updateEndpoint;
    }

    public void setUpdateEndpoint(final String updateEndpoint) {
        this.updateEndpoint = updateEndpoint;
    }

    public String getUpdateToken() {
        return updateToken;
    }

    public void setUpdateToken(final String updateToken) {
        this.updateToken = updateToken;
    }

    public String getSearchProfile() {
        return searchProfile;
    }

    public void setSearchProfile(final String searchProfile) {
        this.searchProfile = searchProfile;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
