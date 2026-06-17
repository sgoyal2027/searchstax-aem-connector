package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;
import com.searchstax.aem.connector.core.services.SiteRoutingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SiteRoutingService.class)
public class SiteRoutingServiceImpl implements SiteRoutingService {

    @Reference
    private ApiConfigService apiConfigService;

    @Override
    public SiteRoutingResult resolve(final String contentPath) {
        final ApiConfig global = apiConfigService.getConfiguration();
        return new SiteRoutingResult(
                global.getUpdateEndpoint(),
                global.getUpdateToken(),
                null,
                null);
    }
}
