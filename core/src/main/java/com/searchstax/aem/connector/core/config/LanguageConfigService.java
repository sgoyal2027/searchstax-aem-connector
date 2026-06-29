package com.searchstax.aem.connector.core.config;

import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;

import java.util.List;

public interface LanguageConfigService {

    List<LanguageMappingConfig> getLanguageMappings();

    void refreshLanguageMappings();

    /**
     * Maps an AEM language/locale code to the configured SearchStax language suffix.
     * Returns the input unchanged when no enabled mapping matches.
     */
    String mapToSearchStaxLanguage(String aemLanguage);

    /**
     * Resolves language from an asset or content path by matching path segments against
     * enabled AEM language mappings. Returns {@code en} when no match is found.
     */
    String resolveLanguageFromPath(String path);
}
