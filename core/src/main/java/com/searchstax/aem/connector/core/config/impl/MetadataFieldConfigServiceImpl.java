package com.searchstax.aem.connector.core.config.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Component(service = MetadataFieldConfigService.class, immediate = true)
public class MetadataFieldConfigServiceImpl implements MetadataFieldConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataFieldConfigServiceImpl.class);

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/metadatafieldmapping";

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private volatile List<MetadataFieldMappingConfig> cachedMappings =
            Collections.emptyList();

    @Reference
    private ResolverUtil resolverUtil;

    @Activate
    protected void activate() {

        LOG.info(
                "Initializing MetadataFieldConfigService cache");

        if (resolverUtil == null) {
            LOG.warn("ResolverUtil not available yet; metadata cache will load on first access");
            return;
        }

        loadMappings();
    }

    @Override
    public List<MetadataFieldMappingConfig> getMetadataFieldMappings() {
        return cachedMappings;
    }

    @Override
    public void refreshMappings() {

        int oldCount = cachedMappings.size();

        LOG.info(
                "Metadata field mapping configuration updated. Refreshing cache.");

        loadMappings();

        LOG.info(
                "Metadata field mapping cache refreshed successfully. Previous Count={} New Count={}",
                oldCount, cachedMappings.size());
    }

    private void loadMappings() {

        LOG.info("Loading metadata field mappings from configuration");

        if (resolverUtil == null) {
            cachedMappings = Collections.emptyList();
            return;
        }

        try (ResourceResolver resourceResolver =
                     resolverUtil.getServiceResolver()) {

            Resource configResource =
                    resourceResolver.getResource(CONFIG_PATH);

            LOG.debug(
                    "Config path: {}",
                    CONFIG_PATH);

            LOG.debug(
                    "Config resource found: {}",
                    configResource != null);

            if (configResource == null) {

                LOG.warn(
                        "Configuration resource not found at path: {}",
                        CONFIG_PATH);

                cachedMappings=
                    Collections.emptyList();

                return;
            }

            String mappingsJson =
                    configResource.getValueMap().get(
                            "metadataMappings",
                            String.class);

            LOG.debug(
                    "Metadata mappings JSON: {}",
                    mappingsJson);

            if (mappingsJson == null
                    || mappingsJson.trim().isEmpty()) {

                LOG.warn(
                        "No metadata field mappings found in configuration");

                cachedMappings = Collections.emptyList();

                return;
            }

            cachedMappings =
                    objectMapper.readValue(
                            mappingsJson,
                            new TypeReference<List<MetadataFieldMappingConfig>>() {
                            });

            LOG.info(
                    "Successfully loaded {} metadata field mappings",
                    cachedMappings.size());

        } catch (Exception e) {

            LOG.error(
                    "Error while loading metadata field mappings",
                    e);

            cachedMappings = Collections.emptyList();
        }
    }
}
