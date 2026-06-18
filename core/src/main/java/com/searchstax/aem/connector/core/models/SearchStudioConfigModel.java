package com.searchstax.aem.connector.core.models;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

import javax.annotation.PostConstruct;

@Model(adaptables = SlingHttpServletRequest.class)
public class SearchStudioConfigModel {

    @OSGiService
    private ApiConfigService apiConfigService;

    @OSGiService
    private ProtectedValueCodec protectedValueCodec;

    private ApiConfig config;

    @PostConstruct
    protected void init() {
        config = apiConfigService.getConfiguration();
    }

    public String getSearchURL() {
        return config == null ? "" : nullToEmpty(config.getSelectEndpoint());
    }

    public String getSuggesterURL() {
        return config == null ? "" : nullToEmpty(config.getAutoSuggestApi());
    }

    public String getSearchAuth() {
        if (config == null) {
            return "";
        }
        return protectedValueCodec.unprotectIfNeeded(config.getSelectToken());
    }

    public String getTrackApiKey() {
        if (config == null) {
            return "";
        }
        return protectedValueCodec.unprotectIfNeeded(config.getAnalyticsTrackingKey());
    }

    public String getAnalyticsBaseUrl() {
        return config == null ? "" : nullToEmpty(config.getAnalyticsTrackingUrl());
    }

    public String getForwardGeocodingEndpoint() {
        return config == null ? "" : nullToEmpty(config.getForwardGeocodingEndpoint());
    }

    public String getReverseGeocodingEndpoint() {
        return config == null ? "" : nullToEmpty(config.getReverseGeocodingEndpoint());
    }

    public String getDiscoveryApiKey() {
        if (config == null) {
            return "";
        }
        return protectedValueCodec.unprotectIfNeeded(config.getDiscoveryApiKey());
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
