package com.searchstax.aem.connector.core.schedulers;

import com.searchstax.aem.connector.core.services.FullIndexAuditService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.testutil.OsgiFieldInjector;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingAuditCleanupSchedulerTest {

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private FullIndexAuditService fullIndexAuditService;

    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduleOptions scheduleOptions;

    @Mock
    private IndexingAuditCleanupScheduler.Config config;

    private final IndexingAuditCleanupScheduler cleanupScheduler = new IndexingAuditCleanupScheduler();

    @BeforeEach
    void setUp() {
        OsgiFieldInjector.inject(cleanupScheduler, "indexingAuditService", indexingAuditService);
        OsgiFieldInjector.inject(cleanupScheduler, "fullIndexAuditService", fullIndexAuditService);
        OsgiFieldInjector.inject(cleanupScheduler, "scheduler", scheduler);
    }

    @Test
    void activate_schedulesCleanupJob() {
        when(config.scheduler_expression()).thenReturn("0 0 2 * * ?");
        when(config.retention_hours()).thenReturn(24);
        when(scheduler.EXPR("0 0 2 * * ?")).thenReturn(scheduleOptions);
        when(scheduleOptions.name("searchstax-indexing-audit-cleanup")).thenReturn(scheduleOptions);
        when(scheduleOptions.canRunConcurrently(false)).thenReturn(scheduleOptions);

        cleanupScheduler.activate(config);

        verify(scheduler).schedule(cleanupScheduler, scheduleOptions);
    }

    @Test
    void run_purgesIncrementalAndFullIndexAuditStores() {
        when(config.scheduler_expression()).thenReturn("0 0 2 * * ?");
        when(config.retention_hours()).thenReturn(48);
        when(scheduler.EXPR("0 0 2 * * ?")).thenReturn(scheduleOptions);
        when(scheduleOptions.name("searchstax-indexing-audit-cleanup")).thenReturn(scheduleOptions);
        when(scheduleOptions.canRunConcurrently(false)).thenReturn(scheduleOptions);
        cleanupScheduler.activate(config);

        cleanupScheduler.run();

        verify(indexingAuditService).purgeOlderThanHours(48);
        verify(fullIndexAuditService).purgeOlderThanHours(48);
    }

    @Test
    void deactivate_unschedulesCleanupJob() {
        cleanupScheduler.deactivate();

        verify(scheduler).unschedule("searchstax-indexing-audit-cleanup");
    }
}
