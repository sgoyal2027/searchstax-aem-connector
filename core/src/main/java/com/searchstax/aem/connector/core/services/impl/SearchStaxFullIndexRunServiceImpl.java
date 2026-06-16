package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexOrchestratorService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SearchStaxFullIndexRunService.class)
public class SearchStaxFullIndexRunServiceImpl implements SearchStaxFullIndexRunService {

    @Reference
    private SearchStaxFullIndexOrchestratorService orchestratorService;

    @Override
    public FullIndexTriggerResult triggerFullIndex(final FullIndexPathConfig config) {
        return orchestratorService.triggerFullIndex(config);
    }

    @Override
    public FullIndexProgress getProgress() {
        return orchestratorService.getProgress();
    }
}
