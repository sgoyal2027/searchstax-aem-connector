package com.searchstax.aem.connector.core.config.model;

/**
 * Resolved SearchStax update target for a content path.
 */
public class SiteRoutingResult {

    private final String updateEndpoint;
    private final String updateToken;
    private final String searchProfile;
    private final String matchedSiteRoot;

    public SiteRoutingResult(
            final String updateEndpoint,
            final String updateToken,
            final String searchProfile,
            final String matchedSiteRoot) {
        this.updateEndpoint = updateEndpoint;
        this.updateToken = updateToken;
        this.searchProfile = searchProfile;
        this.matchedSiteRoot = matchedSiteRoot;
    }

    public String getUpdateEndpoint() {
        return updateEndpoint;
    }

    public String getUpdateToken() {
        return updateToken;
    }

    public String getSearchProfile() {
        return searchProfile;
    }

    public String getMatchedSiteRoot() {
        return matchedSiteRoot;
    }
}
