package com.searchstax.aem.connector.core.schedulers;

import com.searchstax.aem.connector.core.services.FullIndexAuditService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Runnable.class, property = {"scheduler.concurrent=false"})
@Designate(ocd = IndexingAuditCleanupScheduler.Config.class)
public class IndexingAuditCleanupScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingAuditCleanupScheduler.class);

    private static final String SCHEDULER_NAME = "searchstax-indexing-audit-cleanup";

    @ObjectClassDefinition(name = "SearchStax Indexing Audit Cleanup Scheduler")
    public @interface Config {

        @AttributeDefinition(name = "Scheduler expression", description = "Cron expression")
        String scheduler_expression() default "0 0 2 * * ?";

        @AttributeDefinition(name = "Retention hours", description = "Purge audit events older than this many hours")
        int retention_hours() default 24;
    }

    @Reference
    private IndexingAuditService indexingAuditService;

    @Reference
    private FullIndexAuditService fullIndexAuditService;

    @Reference
    private Scheduler scheduler;

    private int retentionHours;

    @Activate
    @Modified
    protected void activate(final Config config) {
        this.retentionHours = config.retention_hours();

        final ScheduleOptions options = scheduler.EXPR(config.scheduler_expression());
        options.name(SCHEDULER_NAME);
        options.canRunConcurrently(false);
        scheduler.schedule(this, options);

        LOG.info("IndexingAuditCleanupScheduler scheduled. Retention={}h", retentionHours);
    }

    @Deactivate
    protected void deactivate() {
        scheduler.unschedule(SCHEDULER_NAME);
    }

    @Override
    public void run() {
        LOG.info("Running indexing audit cleanup. Retention={}h", retentionHours);
        indexingAuditService.purgeOlderThanHours(retentionHours);
        fullIndexAuditService.purgeOlderThanHours(retentionHours);
    }
}
