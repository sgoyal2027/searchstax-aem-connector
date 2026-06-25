package com.searchstax.aem.connector.core.schedulers;

import com.searchstax.aem.connector.core.config.model.IncrementalIndexSchedulerConfig;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.services.IncrementalIndexingService;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component(
        service = Runnable.class,
        property = {
                "scheduler.concurrent=false"
        })
@Designate(ocd = IncrementalIndexSchedulerConfig.class)
public class IncrementalIndexScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalIndexScheduler.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    private int batchSize;

    @Reference
    private IncrementalQueueService queueService;

    @Reference
    private IncrementalIndexingService incrementalIndexingService;

    @Reference
    private ResolverUtil resolverUtil;

    @Reference
    private Scheduler scheduler;

    @Activate
    @Modified
    protected void activate(final IncrementalIndexSchedulerConfig config) {
        this.batchSize = config.batch_size();

        LOG.info("IncrementalIndexScheduler configured. Batch Size={}", batchSize);

        final ScheduleOptions options = scheduler.EXPR(config.scheduler_expression());
        options.name("searchstax-incremental-index-scheduler");
        options.canRunConcurrently(false);
        scheduler.schedule(this, options);

        LOG.info("IncrementalIndexScheduler scheduled");
    }

    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) {
            LOG.debug("Previous incremental indexing execution still running");
            return;
        }

        try {
            processQueue();
        } finally {
            running.set(false);
        }
    }

    private void processQueue() {
        final int queueSize = queueService.size();
        if (queueSize == 0) {
            LOG.debug("Incremental queue is empty");
            return;
        }

        LOG.info("Starting incremental indexing. Queue Size={}", queueSize);

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            if (resolver == null) {
                LOG.error("Unable to obtain service resolver");
                return;
            }

            while (queueService.size() > 0) {
                final int beforeSize = queueService.size();
                final List<IndexRequest> batch = queueService.getBatch(batchSize);

                if (batch.isEmpty()) {
                    break;
                }

                incrementalIndexingService.processBatch(resolver, batch);

                final int afterSize = queueService.size();
                if (afterSize >= beforeSize) {
                    LOG.error(
                            "No queue progress detected. Before={} After={}. Stopping execution to avoid infinite loop.",
                            beforeSize,
                            afterSize);
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("Error executing incremental indexing scheduler", e);
        }
    }
}
