package com.searchstax.aem.connector.core.config;

import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults.TraversalMode;

/**
 * OSGi-backed runtime tuning for full-index execution.
 */
public interface SearchStaxFullIndexRuntimeConfigService {

    int getBatchSize();

    int getQueryPageSize();

    TraversalMode getTraversalMode();

    long getCommitWithinMs();

    int getHardCommitEveryNBatches();

    boolean isHardCommitOnSuccess();

    long getBatchThrottleMs();

    int getResolverRefreshEveryNBatches();
}
