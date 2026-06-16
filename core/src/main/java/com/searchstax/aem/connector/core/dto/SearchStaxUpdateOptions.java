package com.searchstax.aem.connector.core.dto;

/** * Options for SearchStax update API POST requests.
 */
public final class SearchStaxUpdateOptions {

    private final boolean hardCommit;
    private final Long commitWithinMs;
    private final String updateEndpoint;
    private final String updateToken;
    private final String searchProfile;

    private SearchStaxUpdateOptions(
            final boolean hardCommit,
            final Long commitWithinMs,
            final String updateEndpoint,
            final String updateToken,
            final String searchProfile) {
        this.hardCommit = hardCommit;
        this.commitWithinMs = commitWithinMs;
        this.updateEndpoint = updateEndpoint;
        this.updateToken = updateToken;
        this.searchProfile = searchProfile;
    }

    public static SearchStaxUpdateOptions incrementalDefault() {
        return new SearchStaxUpdateOptions(true, null, null, null, null);
    }

    public static SearchStaxUpdateOptions fullIndexBatch(final boolean hardCommit, final long commitWithinMs) {
        return new SearchStaxUpdateOptions(hardCommit, commitWithinMs, null, null, null);
    }

    public static SearchStaxUpdateOptions routed(
            final boolean hardCommit,
            final Long commitWithinMs,
            final String updateEndpoint,
            final String updateToken,
            final String searchProfile) {
        return new SearchStaxUpdateOptions(hardCommit, commitWithinMs, updateEndpoint, updateToken, searchProfile);
    }

    public boolean isHardCommit() {
        return hardCommit;
    }

    public Long getCommitWithinMs() {
        return commitWithinMs;
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
}
