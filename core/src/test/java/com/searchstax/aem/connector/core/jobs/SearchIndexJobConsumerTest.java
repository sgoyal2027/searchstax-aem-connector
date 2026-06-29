package com.searchstax.aem.connector.core.jobs;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchIndexJobConsumerTest {

    @Mock
    private IncrementalQueueService queueService;

    @Mock
    private Job job;

    @InjectMocks
    private SearchIndexJobConsumer consumer;

    @Test
    void process_validActivateJob_enqueuesRequest() {
        when(job.getProperty("path", String.class)).thenReturn("/content/wknd/en/page");
        when(job.getProperty("actionType", String.class)).thenReturn("ACTIVATE");
        when(job.getId()).thenReturn("job-1");
        when(queueService.size()).thenReturn(1);

        assertEquals(JobResult.OK, consumer.process(job));

        verify(queueService).addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, "job-1");
    }

    @Test
    void process_invalidAction_cancelsJob() {
        when(job.getProperty("path", String.class)).thenReturn("/content/wknd/en/page");
        when(job.getProperty("actionType", String.class)).thenReturn("INVALID");

        assertEquals(JobResult.CANCEL, consumer.process(job));

        verify(queueService, never()).addRequest(any(), any(), any());
    }

    @Test
    void process_nullPath_cancelsJob() {
        when(job.getProperty("path", String.class)).thenReturn(null);
        when(job.getProperty("actionType", String.class)).thenReturn("ACTIVATE");

        assertEquals(JobResult.CANCEL, consumer.process(job));

        verify(queueService, never()).addRequest(any(), any(), any());
    }

    @Test
    void process_deleteAction_enqueuesRequest() {
        when(job.getProperty("path", String.class)).thenReturn("/content/wknd/en/page");
        when(job.getProperty("actionType", String.class)).thenReturn("DELETE");
        when(job.getId()).thenReturn("job-delete");

        assertEquals(JobResult.OK, consumer.process(job));

        verify(queueService).addRequest("/content/wknd/en/page", ReplicationActionType.DELETE, "job-delete");
    }

    @Test
    void process_deactivateAction_enqueuesRequest() {
        when(job.getProperty("path", String.class)).thenReturn("/content/wknd/en/page");
        when(job.getProperty("actionType", String.class)).thenReturn("DEACTIVATE");
        when(job.getId()).thenReturn("job-deactivate");

        assertEquals(JobResult.OK, consumer.process(job));

        verify(queueService).addRequest("/content/wknd/en/page", ReplicationActionType.DEACTIVATE, "job-deactivate");
    }
}
