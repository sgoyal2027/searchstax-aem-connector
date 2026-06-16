package com.searchstax.aem.connector.core.config.model;

public class ApiConfig {

    private String endpointUrl;
    private String apiToken;
    private String selectEndpoint;
    private String selectToken;
    private String updateEndpoint;
    private String updateToken;
    private String autoSuggestApi;
    private String relatedSearchesEndpoint;
    private String popularSearchesEndpoint;
    private String discoveryApiKey;
    private String analyticsTrackingUrl;
    private String analyticsTrackingKey;
    private String analyticsReportingUrl;
    private String analyticsReportingApiKey;
    private String forwardGeocodingEndpoint;
    private String reverseGeocodingEndpoint;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(final String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(final String apiToken) {
        this.apiToken = apiToken;
    }

    public String getSelectEndpoint() {
        return selectEndpoint;
    }

    public void setSelectEndpoint(final String selectEndpoint) {
        this.selectEndpoint = selectEndpoint;
    }

    public String getSelectToken() {
        return selectToken;
    }

    public void setSelectToken(final String selectToken) {
        this.selectToken = selectToken;
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

    public String getAutoSuggestApi() {
        return autoSuggestApi;
    }

    public void setAutoSuggestApi(final String autoSuggestApi) {
        this.autoSuggestApi = autoSuggestApi;
    }

    public String getRelatedSearchesEndpoint() {
        return relatedSearchesEndpoint;
    }

    public void setRelatedSearchesEndpoint(final String relatedSearchesEndpoint) {
        this.relatedSearchesEndpoint = relatedSearchesEndpoint;
    }

    public String getPopularSearchesEndpoint() {
        return popularSearchesEndpoint;
    }

    public void setPopularSearchesEndpoint(final String popularSearchesEndpoint) {
        this.popularSearchesEndpoint = popularSearchesEndpoint;
    }

    public String getDiscoveryApiKey() {
        return discoveryApiKey;
    }

    public void setDiscoveryApiKey(final String discoveryApiKey) {
        this.discoveryApiKey = discoveryApiKey;
    }

    public String getAnalyticsTrackingUrl() {
        return analyticsTrackingUrl;
    }

    public void setAnalyticsTrackingUrl(final String analyticsTrackingUrl) {
        this.analyticsTrackingUrl = analyticsTrackingUrl;
    }

    public String getAnalyticsTrackingKey() {
        return analyticsTrackingKey;
    }

    public void setAnalyticsTrackingKey(final String analyticsTrackingKey) {
        this.analyticsTrackingKey = analyticsTrackingKey;
    }

    public String getAnalyticsReportingUrl() {
        return analyticsReportingUrl;
    }

    public void setAnalyticsReportingUrl(final String analyticsReportingUrl) {
        this.analyticsReportingUrl = analyticsReportingUrl;
    }

    public String getAnalyticsReportingApiKey() {
        return analyticsReportingApiKey;
    }

    public void setAnalyticsReportingApiKey(final String analyticsReportingApiKey) {
        this.analyticsReportingApiKey = analyticsReportingApiKey;
    }

    public String getForwardGeocodingEndpoint() {
        return forwardGeocodingEndpoint;
    }

    public void setForwardGeocodingEndpoint(final String forwardGeocodingEndpoint) {
        this.forwardGeocodingEndpoint = forwardGeocodingEndpoint;
    }

    public String getReverseGeocodingEndpoint() {
        return reverseGeocodingEndpoint;
    }

    public void setReverseGeocodingEndpoint(final String reverseGeocodingEndpoint) {
        this.reverseGeocodingEndpoint = reverseGeocodingEndpoint;
    }
}
