package com.searchstax.aem.connector.core.jobs;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Component(service = {JobConsumer.class}, property = {"job.topics=searchstaxconnector/incremental-index"})
public class SearchIndexJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SearchIndexJobConsumer.class);

    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
            ReplicationActionType.ACTIVATE.name(),
            ReplicationActionType.DEACTIVATE.name(),
            ReplicationActionType.DELETE.name());

    @Reference
    private IncrementalQueueService queueService;

    @Reference
    private IndexingAuditService indexingAuditService;

    @Override
    public JobResult process(final Job job) {
        LOG.info("SearchIndexJobConsumer invoked");

        final String path = job.getProperty("path", String.class);
        final String action = job.getProperty("actionType", String.class);

        LOG.info("Received job. Path={} Action={}", path, action);

        if (path == null || action == null) {
            LOG.error("Invalid job properties");
            return JobResult.CANCEL;
        }

        if (!SUPPORTED_ACTIONS.contains(action)) {
            LOG.error("Unknown actionType: {}", action);
            return JobResult.CANCEL;
        }

        final ReplicationActionType actionType;
        try {
            actionType = ReplicationActionType.valueOf(action);
        } catch (IllegalArgumentException e) {
            LOG.error("Unknown actionType: {}", action, e);
            return JobResult.CANCEL;
        }

        queueService.addRequest(path, actionType, job.getId());

        indexingAuditService.recordEvent(
                path,
                actionType.name(),
                IndexingAuditService.STATUS_QUEUED,
                "Added to incremental queue",
                job.getId(),
                0,
                null);

        LOG.info("Request added to queue. Queue Size={}", queueService.size());

        return JobResult.OK;
    }
}
