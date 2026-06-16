package com.searchstax.aem.connector.core.config;

import com.searchstax.aem.connector.core.config.model.SiteApplicationMappingConfig;

import java.util.List;

public interface SiteApplicationConfigService {

    List<SiteApplicationMappingConfig> getSiteMappings();

    void refreshSiteMappings();
}
