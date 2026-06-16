package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ApiConfigService.class)
public class ApiConfigServiceImpl implements ApiConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(ApiConfigServiceImpl.class);

    public static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/apiconfig";

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public ApiConfig getConfiguration() {
        final ApiConfig config = new ApiConfig();
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource resource = resolver.getResource(CONFIG_PATH);
            if (resource == null) {
                LOG.warn("API configuration not found at {}", CONFIG_PATH);
                return config;
            }
            final ValueMap vm = resource.getValueMap();
            config.setEndpointUrl(vm.get("endpointUrl", ""));
            config.setApiToken(vm.get("apiToken", ""));
            config.setSelectEndpoint(vm.get("selectEndpoint", ""));
            config.setSelectToken(vm.get("selectToken", ""));
            config.setUpdateEndpoint(vm.get("updateEndpoint", ""));
            config.setUpdateToken(vm.get("updateToken", ""));
            config.setAutoSuggestApi(vm.get("autoSuggestApi", ""));
            config.setRelatedSearchesEndpoint(vm.get("relatedSearchesEndpoint", ""));
            config.setPopularSearchesEndpoint(vm.get("popularSearchesEndpoint", ""));
            config.setDiscoveryApiKey(vm.get("discoveryApiKey", ""));
            config.setAnalyticsTrackingUrl(vm.get("analyticsTrackingUrl", ""));
            config.setAnalyticsTrackingKey(vm.get("analyticsTrackingKey", ""));
            config.setAnalyticsReportingUrl(vm.get("analyticsReportingUrl", ""));
            config.setAnalyticsReportingApiKey(vm.get("analyticsReportingApiKey", ""));
            config.setForwardGeocodingEndpoint(vm.get("forwardGeocodingEndpoint", ""));
            config.setReverseGeocodingEndpoint(vm.get("reverseGeocodingEndpoint", ""));
        } catch (LoginException e) {
            LOG.error("Unable to load API configuration", e);
        }
        return config;
    }
}
