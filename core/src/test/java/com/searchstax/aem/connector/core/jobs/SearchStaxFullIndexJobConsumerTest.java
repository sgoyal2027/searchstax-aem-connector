package com.searchstax.aem.connector.core.jobs;

import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexExecutionService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchStaxFullIndexJobConsumerTest {

    @InjectMocks
    private SearchStaxFullIndexJobConsumer consumer;

    @Mock
    private SearchStaxFullIndexExecutionService executionService;

    @Mock
    private SlingSettingsService slingSettings;

    @Mock
    private Job job;

    @Test
    void testProcessPublishInstance() {

        when(slingSettings.getRunModes())
                .thenReturn(Collections.singleton("publish"));

        JobResult result = consumer.process(job);

        assertEquals(JobResult.CANCEL, result);

        verifyNoInteractions(executionService);
    }

    @Test
    void testProcessSuccess() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        when(job.getId()).thenReturn("job-1");
        when(job.getPropertyNames())
                .thenReturn(Set.of());

        FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.SUCCESS,
                        10,
                        10,
                        0,
                        8,
                        2,
                        1,
                        "/content/site",
                        1L,
                        100L,
                        "Completed");

        when(executionService.getProgressSnapshot())
                .thenReturn(progress);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.OK, result);

        verify(executionService)
                .execute(any());

        verify(executionService)
                .getProgressSnapshot();
    }

    @Test
    void testProcessFailedState() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        when(job.getId()).thenReturn("job-2");
        when(job.getPropertyNames())
                .thenReturn(Set.of());

        FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.FAILED,
                        10,
                        5,
                        5,
                        3,
                        2,
                        1,
                        "/content/site",
                        1L,
                        100L,
                        "Failed");

        when(executionService.getProgressSnapshot())
                .thenReturn(progress);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.FAILED, result);

        verify(executionService)
                .execute(any());
    }

    @Test
    void testProcessPartialFailureState() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        when(job.getId()).thenReturn("job-3");
        when(job.getPropertyNames())
                .thenReturn(Set.of());

        FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.PARTIAL_FAILURE,
                        10,
                        8,
                        2,
                        6,
                        2,
                        1,
                        "/content/site",
                        1L,
                        100L,
                        "Partial");

        when(executionService.getProgressSnapshot())
                .thenReturn(progress);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.OK, result);
    }

    @Test
    void testProcessException() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        when(job.getId()).thenReturn("job-4");
        when(job.getPropertyNames())
                .thenReturn(Set.of());

        doThrow(new RuntimeException("Execution Failed"))
                .when(executionService)
                .execute(any());

        JobResult result = consumer.process(job);

        assertEquals(JobResult.FAILED, result);
    }
}