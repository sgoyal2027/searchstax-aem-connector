package com.searchstax.aem.connector.core.config.wizard;

/**
 * Synthetic resource paths for Touch UI wizards backed by OSGi Configuration Admin.
 */
public final class SearchStaxWizardBindingPaths {

    public static final String ROOT = "/apps/searchstaxconnector/wizard-bindings";

    public static final String API_PAGE = ROOT + "/api";

    public static final String API_JCR_CONTENT = API_PAGE + "/jcr:content";

    public static final String FULL_INDEX_PAGE = ROOT + "/fullindex";

    public static final String FULL_INDEX_JCR_CONTENT = FULL_INDEX_PAGE + "/jcr:content";

    private static final String SERVLET_BASE = "/bin/searchstaxconnector/wizard";

    /**
     * POST targets for Granite forms. Using {@code /bin/...} avoids Sling's default POST servlet
     * attempting to create JCR nodes under the synthetic {@link #ROOT} tree.
     */
    public static final String SERVLET_API_SAVE = SERVLET_BASE + "/api-save";

    public static final String SERVLET_FULL_INDEX_SAVE = SERVLET_BASE + "/fullindex-save";

    public static final String SERVLET_FULL_INDEX_RUN = SERVLET_BASE + "/fullindex-run";

    public static final String SERVLET_FULL_INDEX_STATUS = SERVLET_BASE + "/fullindex-status";

    private SearchStaxWizardBindingPaths() {
    }
}
