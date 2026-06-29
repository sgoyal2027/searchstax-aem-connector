package com.searchstax.aem.connector.core.config.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import com.searchstax.aem.connector.core.utils.LanguageMappingConfigUtil;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

@Component(service = LanguageConfigService.class)
public class LanguageConfigServiceImpl implements LanguageConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataFieldConfigServiceImpl.class);

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private volatile List<LanguageMappingConfig> cachedMappings =
            Collections.emptyList();

    @Reference
    private ResolverUtil resolverUtil;

    @Activate
    protected void activate() {

        LOG.info(
                "Initializing Language config service and loading mappings cache");

        loadMappings();
    }

    @Override
    public List<LanguageMappingConfig> getLanguageMappings() {
        return cachedMappings;
    }

    @Override
    public void refreshLanguageMappings() {

        int oldCount = cachedMappings.size();

        LOG.info(
                "Language field mapping configuration updated. Refreshing cache.");

        loadMappings();

        LOG.info(
                "Language field mapping cache refreshed successfully. Previous Count={} New Count={}",
                oldCount, cachedMappings.size());
    }

    @Override
    public String mapToSearchStaxLanguage(final String aemLanguage) {
        if (aemLanguage == null || aemLanguage.isBlank()) {
            return "en";
        }

        final String normalized = aemLanguage.trim().toLowerCase(Locale.ROOT);

        for (final LanguageMappingConfig mapping : cachedMappings) {
            if (!mapping.isEnabledLanguageMapping()) {
                continue;
            }

            final String configuredAem = resolveConfiguredAemLanguage(mapping);
            if (configuredAem == null || configuredAem.isBlank()) {
                continue;
            }

            if (normalized.equals(configuredAem.trim().toLowerCase(Locale.ROOT))) {
                final String searchStaxLanguage = mapping.getSearchStaxLanguage();
                return searchStaxLanguage != null && !searchStaxLanguage.isBlank()
                        ? searchStaxLanguage.trim()
                        : aemLanguage;
            }
        }

        return aemLanguage;
    }

    @Override
    public String resolveLanguageFromPath(final String path) {
        if (path == null || path.isBlank()) {
            return "en";
        }

        for (final String segment : path.split("/")) {
            if (segment.isBlank()) {
                continue;
            }

            final String resolvedLanguage = resolveSegmentToSearchStaxLanguage(segment);
            if (resolvedLanguage != null) {
                return resolvedLanguage;
            }
        }

        return "en";
    }

    private String resolveSegmentToSearchStaxLanguage(final String segment) {
        final String normalized =
                segment.trim().toLowerCase(Locale.ROOT);

        final String mappedLanguage = mapConfiguredAemLanguage(normalized);
        if (mappedLanguage != null) {
            return mappedLanguage;
        }

        final String languageCode = resolveLanguageNameToCode(normalized);
        if (languageCode == null) {
            return null;
        }

        final String mappedFromCode = mapConfiguredAemLanguage(languageCode);
        return mappedFromCode != null
                ? mappedFromCode
                : mapToSearchStaxLanguage(languageCode);
    }

    private String mapConfiguredAemLanguage(final String normalizedSegment) {
        for (final LanguageMappingConfig mapping : cachedMappings) {
            if (!mapping.isEnabledLanguageMapping()) {
                continue;
            }

            final String configuredAem = resolveConfiguredAemLanguage(mapping);
            if (configuredAem == null || configuredAem.isBlank()) {
                continue;
            }

            if (normalizedSegment.equals(configuredAem.trim().toLowerCase(Locale.ROOT))) {
                final String searchStaxLanguage = mapping.getSearchStaxLanguage();
                return searchStaxLanguage != null && !searchStaxLanguage.isBlank()
                        ? searchStaxLanguage.trim()
                        : configuredAem;
            }
        }

        return null;
    }

    private static String resolveLanguageNameToCode(final String normalizedSegment) {
        for (final Locale locale : Locale.getAvailableLocales()) {
            final String language = locale.getLanguage();
            if (language == null || language.isBlank()) {
                continue;
            }

            final String displayName = locale.getDisplayLanguage(Locale.ENGLISH);
            if (displayName != null
                    && normalizedSegment.equals(displayName.trim().toLowerCase(Locale.ROOT))) {
                return language;
            }

            try {
                final String iso3Language = locale.getISO3Language();
                if (iso3Language != null
                        && normalizedSegment.equals(iso3Language.trim().toLowerCase(Locale.ROOT))) {
                    return language;
                }
            } catch (final MissingResourceException ignored) {
                // Locale has no ISO-639-2 code; skip.
            }
        }

        return null;
    }

    private static String resolveConfiguredAemLanguage(final LanguageMappingConfig mapping) {
        if ("custom".equalsIgnoreCase(mapping.getAemLanguage())) {
            return mapping.getCustomAemLanguage();
        }
        return mapping.getAemLanguage();
    }

    private void loadMappings() {

        LOG.info("Loading Language field mappings from configuration");

        try (ResourceResolver resourceResolver =
                     resolverUtil.getServiceResolver()) {

            final String mappingsJson =
                    LanguageMappingConfigUtil.loadOrPersistDefaultMappingsJson(resourceResolver);

            LOG.debug(
                    "Language mappings JSON: {}",
                    mappingsJson);

            cachedMappings =
                    objectMapper.readValue(
                            mappingsJson,
                            new TypeReference<List<LanguageMappingConfig>>() {
                            });

            LOG.info(
                    "Successfully loaded {} language field mappings",
                    cachedMappings.size());

        } catch (PersistenceException e) {

            LOG.error(
                    "Failed to persist default language field mappings",
                    e);

            cachedMappings = LanguageMappingConfig.defaultMappings();

        } catch (Exception e) {

            LOG.error(
                    "Error while loading language field mappings",
                    e);

            cachedMappings = Collections.emptyList();
        }
    }
    
}
