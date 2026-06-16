package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.SearchStaxFullIndexRuntimeConfigService;
import com.searchstax.aem.connector.core.config.SearchStaxFullIndexRuntimeConfiguration;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults.TraversalMode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = SearchStaxFullIndexRuntimeConfigService.class,
        configurationPid = "com.searchstax.aem.connector.core.config.SearchStaxFullIndexRuntimeConfiguration")
@Designate(ocd = SearchStaxFullIndexRuntimeConfiguration.class)
public class SearchStaxFullIndexRuntimeConfigServiceImpl implements SearchStaxFullIndexRuntimeConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxFullIndexRuntimeConfigServiceImpl.class);

    private int batchSize;
    private int queryPageSize;
    private TraversalMode traversalMode;
    private long commitWithinMs;
    private int hardCommitEveryNBatches;
    private boolean hardCommitOnSuccess;
    private long batchThrottleMs;
    private int resolverRefreshEveryNBatches;

    @Activate
    @Modified
    protected void activate(final SearchStaxFullIndexRuntimeConfiguration config) {
        this.batchSize = config.batchSize();
        this.queryPageSize = config.queryPageSize();
        this.traversalMode = parseTraversalMode(config.traversalMode());
        this.commitWithinMs = config.commitWithinMs();
        this.hardCommitEveryNBatches = config.hardCommitEveryNBatches();
        this.hardCommitOnSuccess = config.hardCommitOnSuccess();
        this.batchThrottleMs = config.batchThrottleMs();
        this.resolverRefreshEveryNBatches = config.resolverRefreshEveryNBatches();

        LOG.info(
                "Full-index runtime config activated: batchSize={} traversalMode={} commitWithinMs={}",
                batchSize,
                traversalMode,
                commitWithinMs);
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public int getQueryPageSize() {
        return queryPageSize;
    }

    @Override
    public TraversalMode getTraversalMode() {
        return traversalMode;
    }

    @Override
    public long getCommitWithinMs() {
        return commitWithinMs;
    }

    @Override
    public int getHardCommitEveryNBatches() {
        return hardCommitEveryNBatches;
    }

    @Override
    public boolean isHardCommitOnSuccess() {
        return hardCommitOnSuccess;
    }

    @Override
    public long getBatchThrottleMs() {
        return batchThrottleMs;
    }

    @Override
    public int getResolverRefreshEveryNBatches() {
        return resolverRefreshEveryNBatches;
    }

    private static TraversalMode parseTraversalMode(final String value) {
        if (value == null) {
            return TraversalMode.JCR_SQL2;
        }
        try {
            return TraversalMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid traversal mode '{}'; using JCR_SQL2", value);
            return TraversalMode.JCR_SQL2;
        }
    }
}
