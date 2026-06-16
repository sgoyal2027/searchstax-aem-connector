package com.searchstax.aem.connector.core.config;

import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;

import java.util.List;

public interface MetadataFieldConfigService {

    List<MetadataFieldMappingConfig> getMetadataFieldMappings();

    void refreshMappings();
}
