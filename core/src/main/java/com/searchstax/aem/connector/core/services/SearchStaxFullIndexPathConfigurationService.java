package com.searchstax.aem.connector.core.services;



/**

 * Resolves and filters full-index path configuration (normalization, root/include gates, hierarchical excludes).

 */

public interface SearchStaxFullIndexPathConfigurationService {



    /**

     * Include paths normalized, deduped, and gated under the configured root.

     */

    String[] resolveEffectiveIncludes(FullIndexPathConfig config);



    /**

     * Exclude paths normalized, deduped, and gated under the configured root.

     */

    String[] resolveEffectiveExcludes(FullIndexPathConfig config);



    /**

     * Returns true when {@code path} equals an exclude or is a descendant of one (hierarchical match).

     */

    boolean isExcludedPath(String path, String[] effectiveExcludes);

}

