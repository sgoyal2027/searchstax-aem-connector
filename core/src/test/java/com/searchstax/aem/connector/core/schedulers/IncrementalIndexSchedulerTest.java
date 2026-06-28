package com.searchstax.aem.connector.core.schedulers;

import com.searchstax.aem.connector.core.config.model.IncrementalIndexSchedulerConfig;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.services.IncrementalIndexingService;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncrementalIndexSchedulerTest {

    @InjectMocks
    private IncrementalIndexScheduler schedulerService;

    @Mock
    private IncrementalQueueService queueService;

    @Mock
    private IncrementalIndexingService incrementalIndexingService;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduleOptions scheduleOptions;

    @Mock
    private IncrementalIndexSchedulerConfig config;

    @Mock
    private ResourceResolver resolver;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {

        Field field =
                IncrementalIndexScheduler.class.getDeclaredField("batchSize");

        field.setAccessible(true);

        field.set(schedulerService, 10);

    }

    @Test
    void testActivate() {

        when(config.batch_size()).thenReturn(10);
        when(config.scheduler_expression()).thenReturn("0/10 * * * * ?");

        when(scheduler.EXPR("0/10 * * * * ?"))
                .thenReturn(scheduleOptions);

        when(scheduleOptions.name(anyString()))
                .thenReturn(scheduleOptions);

        when(scheduleOptions.canRunConcurrently(anyBoolean()))
                .thenReturn(scheduleOptions);

        schedulerService.activate(config);

        verify(scheduler).EXPR("0/10 * * * * ?");
        verify(scheduleOptions)
                .name("searchstax-incremental-index-scheduler");
        verify(scheduleOptions)
                .canRunConcurrently(false);
        verify(scheduler)
                .schedule(eq(schedulerService), eq(scheduleOptions));
    }

    @Test
    void testRunQueueEmpty() {

        when(queueService.size()).thenReturn(0);

        schedulerService.run();

        verify(queueService).size();
        verifyNoInteractions(resolverUtil);
        verifyNoInteractions(incrementalIndexingService);
    }

    @Test
    void testRunResolverNull() throws Exception {

        when(queueService.size())
                .thenReturn(1);

        when(resolverUtil.getServiceResolver())
                .thenReturn(null);

        schedulerService.run();

        verify(queueService).size();
        verify(resolverUtil).getServiceResolver();
        verifyNoInteractions(incrementalIndexingService);
    }

    @Test
    void testRunProcessSingleBatch() throws Exception {

        IndexRequest request = mock(IndexRequest.class);

        when(queueService.size())
                .thenReturn(1)   // initial queue check
                .thenReturn(1)   // while condition
                .thenReturn(1)   // beforeSize
                .thenReturn(0)   // afterSize
                .thenReturn(0);  // next while condition

        when(queueService.getBatch(10))
                .thenReturn(List.of(request));

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        schedulerService.run();

        verify(queueService).getBatch(10);

        verify(incrementalIndexingService)
                .processBatch(eq(resolver), anyList());

        verify(resolver).close();
    }
}