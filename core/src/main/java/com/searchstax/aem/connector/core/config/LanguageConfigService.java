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
}
