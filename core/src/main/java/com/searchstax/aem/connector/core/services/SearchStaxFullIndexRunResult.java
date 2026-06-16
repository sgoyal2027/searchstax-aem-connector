package com.searchstax.aem.connector.core.services;

/**
 * Outcome of a full-index POST to the SearchStax update endpoint.
 */
public final class SearchStaxFullIndexRunResult {

    private final boolean success;
    private final String message;
    private final int httpStatus;
    private final int pagesIndexed;
    private final int assetsIndexed;
    private final String solrResponseBody;

    public SearchStaxFullIndexRunResult(
            final boolean success,
            final String message,
            final int httpStatus,
            final int pagesIndexed,
            final int assetsIndexed,
            final String solrResponseBody) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.httpStatus = httpStatus;
        this.pagesIndexed = pagesIndexed;
        this.assetsIndexed = assetsIndexed;
        this.solrResponseBody = solrResponseBody == null ? "" : solrResponseBody;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getPagesIndexed() {
        return pagesIndexed;
    }

    public int getAssetsIndexed() {
        return assetsIndexed;
    }

    public String getSolrResponseBody() {
        return solrResponseBody;
    }
}
