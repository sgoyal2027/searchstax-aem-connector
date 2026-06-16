package com.searchstax.aem.connector.core.config.wizard;

/**
 * Legacy {@code /conf} paths previously used by the Touch UI wizards. Read-only fallback when
 * environment-specific OSGi dictionaries are still empty (upgrade scenarios).
 */
public final class SearchStaxLegacyWizardConfPaths {

    public static final String API_JCR_CONTENT = "/conf/global/settings/searchstaxconnector/api/jcr:content";

    public static final String FULL_INDEX_JCR_CONTENT =
            "/conf/global/settings/searchstaxconnector/fullindex/jcr:content";

    private SearchStaxLegacyWizardConfPaths() {
    }
}
