package com.searchstax.aem.connector.core.constants;

/**
 * Full-index job identifiers and shared constants. Runtime tuning is provided by
 * {@link com.searchstax.aem.connector.core.config.SearchStaxFullIndexRuntimeConfigService}.
 */
public final class SearchStaxFullIndexDefaults {

    public static final String JOB_TOPIC = "searchstax/fullindex";
    public static final String JOB_PROP_ROOT_PATH = "rootPath";
    public static final String JOB_PROP_INCLUDE_PATHS = "includePaths";
    public static final String JOB_PROP_EXCLUDE_PATHS = "excludePaths";
    public static final String JOB_PROP_CHILD_PAGES = "childPages";
    public static final String JOB_PROP_INCLUDE_CHILD_PATHS = "includeChildPaths";

    public static final String DAM_ROOT = "/content/dam";

    public enum TraversalMode {
        QUERY_BUILDER,
        JCR_SQL2
    }

    private SearchStaxFullIndexDefaults() {
    }
}
