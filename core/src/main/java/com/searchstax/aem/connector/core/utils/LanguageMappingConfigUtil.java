package com.searchstax.aem.connector.core.utils;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LanguageMappingConfigUtil {

    public static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/languagemapping";

    public static final String PROPERTY_NAME = "languageMappings";

    private static final Gson GSON = new Gson();

    private static final Logger LOG = LoggerFactory.getLogger(LanguageMappingConfigUtil.class);

    private LanguageMappingConfigUtil() {
    }

    /**
     * Reads stored mappings from JCR, persisting the default en mapping when configuration is missing or empty.
     */
    public static String loadOrPersistDefaultMappingsJson(final ResourceResolver resolver)
            throws PersistenceException {

        Resource resource = ConfigResourceUtil.getOrCreateConfigResource(resolver, CONFIG_PATH);
        if (resource == null) {
            LOG.warn(
                    "Language mapping config path not found and could not be created: {}",
                    CONFIG_PATH);
            return GSON.toJson(LanguageMappingConfig.defaultMappings());
        }

        final String stored = resource.getValueMap().get(PROPERTY_NAME, "[]");
        if (!LanguageMappingConfig.isEmptyMappingsJson(stored)) {
            return stored;
        }

        final String defaultsJson = GSON.toJson(LanguageMappingConfig.defaultMappings());
        final ModifiableValueMap properties = ConfigResourceUtil.getModifiableProperties(resource);
        if (properties == null) {
            LOG.warn("Could not adapt language mapping config resource to ModifiableValueMap");
            return defaultsJson;
        }

        properties.put(PROPERTY_NAME, defaultsJson);
        resolver.commit();
        LOG.info("Persisted default language mapping configuration");

        return defaultsJson;
    }
}
