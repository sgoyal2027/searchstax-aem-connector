package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.SearchStaxFullIndexRuntimeConfiguration;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults.TraversalMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxFullIndexRuntimeConfigServiceImplTest {

    @Test
    void activate_exposesConfiguredValues() {
        final SearchStaxFullIndexRuntimeConfiguration config = mock(SearchStaxFullIndexRuntimeConfiguration.class);
        when(config.batchSize()).thenReturn(50);
        when(config.queryPageSize()).thenReturn(250);
        when(config.traversalMode()).thenReturn("QUERY_BUILDER");
        when(config.commitWithinMs()).thenReturn(60_000L);
        when(config.hardCommitEveryNBatches()).thenReturn(5);
        when(config.hardCommitOnSuccess()).thenReturn(false);
        when(config.batchThrottleMs()).thenReturn(100L);
        when(config.resolverRefreshEveryNBatches()).thenReturn(15);

        final SearchStaxFullIndexRuntimeConfigServiceImpl service = new SearchStaxFullIndexRuntimeConfigServiceImpl();
        service.activate(config);

        assertEquals(50, service.getBatchSize());
        assertEquals(250, service.getQueryPageSize());
        assertEquals(TraversalMode.QUERY_BUILDER, service.getTraversalMode());
        assertEquals(60_000L, service.getCommitWithinMs());
        assertEquals(5, service.getHardCommitEveryNBatches());
        assertFalse(service.isHardCommitOnSuccess());
        assertEquals(100L, service.getBatchThrottleMs());
        assertEquals(15, service.getResolverRefreshEveryNBatches());
    }

    @Test
    void activate_defaultsTraversalModeForInvalidValue() {
        final SearchStaxFullIndexRuntimeConfiguration config = mock(SearchStaxFullIndexRuntimeConfiguration.class);
        when(config.batchSize()).thenReturn(100);
        when(config.queryPageSize()).thenReturn(500);
        when(config.traversalMode()).thenReturn("not-a-mode");
        when(config.commitWithinMs()).thenReturn(120_000L);
        when(config.hardCommitEveryNBatches()).thenReturn(10);
        when(config.hardCommitOnSuccess()).thenReturn(true);
        when(config.batchThrottleMs()).thenReturn(300L);
        when(config.resolverRefreshEveryNBatches()).thenReturn(20);

        final SearchStaxFullIndexRuntimeConfigServiceImpl service = new SearchStaxFullIndexRuntimeConfigServiceImpl();
        service.activate(config);

        assertEquals(TraversalMode.JCR_SQL2, service.getTraversalMode());
        assertTrue(service.isHardCommitOnSuccess());
    }
}
