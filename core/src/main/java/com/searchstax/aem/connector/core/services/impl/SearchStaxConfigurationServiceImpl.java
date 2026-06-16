package com.searchstax.aem.connector.core.services.impl;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-only facade over wizard-persisted API configuration ({@link ApiConfigService}).
 * All indexing components reference this service instead of reading configuration directly.
 */
@Component(service = SearchStaxConfigurationService.class)
public class SearchStaxConfigurationServiceImpl implements SearchStaxConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxConfigurationServiceImpl.class);

    @Reference
    private ApiConfigService apiConfigService;

    @Reference
    private CryptoSupport cryptoSupport;

    @Activate
    protected void activate() {
        final ApiConfig config = apiConfigService.getConfiguration();
        LOG.info(
                "SearchStaxConfigurationService activated — updateEndpoint={}",
                config != null ? config.getUpdateEndpoint() : "");
    }

    private String unprotectIfNeeded(final String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (cryptoSupport == null) {
            return value;
        }
        try {
            if (cryptoSupport.isProtected(value)) {
                return cryptoSupport.unprotect(value);
            }
        } catch (final CryptoException e) {
            LOG.warn("Failed to decrypt protected value; returning encrypted value as-is", e);
        }
        return value;
    }

    private ApiConfig config() {
        return apiConfigService.getConfiguration();
    }

    @Override
    public String getEndpointUrl() {
        return config().getEndpointUrl();
    }

    @Override
    public String getApiToken() {
        return unprotectIfNeeded(config().getApiToken());
    }

    @Override
    public String getSelectEndpoint() {
        return config().getSelectEndpoint();
    }

    @Override
    public String getSelectToken() {
        return unprotectIfNeeded(config().getSelectToken());
    }

    @Override
    public String getUpdateEndpoint() {
        return config().getUpdateEndpoint();
    }

    @Override
    public String getUpdateToken() {
        return unprotectIfNeeded(config().getUpdateToken());
    }

    @Override
    public String getAutoSuggestApi() {
        return config().getAutoSuggestApi();
    }

    @Override
    public String getRelatedSearchesEndpoint() {
        return config().getRelatedSearchesEndpoint();
    }

    @Override
    public String getPopularSearchesEndpoint() {
        return config().getPopularSearchesEndpoint();
    }

    @Override
    public String getDiscoveryApiKey() {
        return unprotectIfNeeded(config().getDiscoveryApiKey());
    }

    @Override
    public String getAnalyticsTrackingUrl() {
        return config().getAnalyticsTrackingUrl();
    }

    @Override
    public String getAnalyticsTrackingKey() {
        return unprotectIfNeeded(config().getAnalyticsTrackingKey());
    }

    @Override
    public String getAnalyticsReportingUrl() {
        return config().getAnalyticsReportingUrl();
    }

    @Override
    public String getAnalyticsReportingApiKey() {
        return unprotectIfNeeded(config().getAnalyticsReportingApiKey());
    }

    @Override
    public String getForwardGeocodingEndpoint() {
        return config().getForwardGeocodingEndpoint();
    }

    @Override
    public String getReverseGeocodingEndpoint() {
        return config().getReverseGeocodingEndpoint();
    }
}
