package com.searchstax.aem.connector.core.config.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.SiteApplicationConfigService;
import com.searchstax.aem.connector.core.config.model.SiteApplicationMappingConfig;
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

@Component(service = SiteApplicationConfigService.class)
public class SiteApplicationConfigServiceImpl implements SiteApplicationConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteApplicationConfigServiceImpl.class);

    public static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/siteapplicationmapping";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<SiteApplicationMappingConfig> cachedMappings = Collections.emptyList();

    @Reference
    private ResolverUtil resolverUtil;

    @Activate
    protected void activate() {
        loadMappings();
    }

    @Override
    public List<SiteApplicationMappingConfig> getSiteMappings() {
        return cachedMappings;
    }

    @Override
    public void refreshSiteMappings() {
        loadMappings();
    }

    private void loadMappings() {
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource configResource = resolver.getResource(CONFIG_PATH);
            if (configResource == null) {
                cachedMappings = Collections.emptyList();
                return;
            }

            final String mappingsJson = configResource.getValueMap().get("siteMappings", String.class);
            if (mappingsJson == null || mappingsJson.isBlank()) {
                cachedMappings = Collections.emptyList();
                return;
            }

            cachedMappings = objectMapper.readValue(
                    mappingsJson, new TypeReference<List<SiteApplicationMappingConfig>>() {});
            LOG.info("Loaded {} site application mappings", cachedMappings.size());
        } catch (Exception e) {
            LOG.error("Error loading site application mappings", e);
            cachedMappings = Collections.emptyList();
        }
    }
}
