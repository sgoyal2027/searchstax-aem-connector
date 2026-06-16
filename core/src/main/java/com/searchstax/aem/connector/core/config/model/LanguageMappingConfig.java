package com.searchstax.aem.connector.core.config.model;

public class LanguageMappingConfig {

    private String aemLanguage;
    private String customAemLanguage;
    private String searchStaxLanguage;
    private boolean enabledLanguageMapping;

    public String getAemLanguage() {
        return aemLanguage;
    }

    public void setAemLanguage(final String aemLanguage) {
        this.aemLanguage = aemLanguage;
    }

    public String getCustomAemLanguage() {
        return customAemLanguage;
    }

    public void setCustomAemLanguage(String customAemLanguage) {
        this.customAemLanguage = customAemLanguage;
    }

    public String getSearchStaxLanguage() {
        return searchStaxLanguage;
    }

    public void setSearchStaxLanguage(final String searchStaxLanguage) {
        this.searchStaxLanguage = searchStaxLanguage;
    }

    public boolean isEnabledLanguageMapping() {
        return enabledLanguageMapping;
    }

    public void setEnabledLanguageMapping(final boolean enabledLanguageMapping) {
        this.enabledLanguageMapping = enabledLanguageMapping;
    }
}
