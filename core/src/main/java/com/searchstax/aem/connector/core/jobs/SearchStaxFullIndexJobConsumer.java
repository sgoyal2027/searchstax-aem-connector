package com.searchstax.aem.connector.core.jobs;

import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexExecutionService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = JobConsumer.class,
        property = {
            JobConsumer.PROPERTY_TOPICS + "=" + SearchStaxFullIndexDefaults.JOB_TOPIC,
            Job.PROPERTY_JOB_RETRIES + ":Integer=0"
        })
@SuppressWarnings("CQRules:AMSCORE-553")
public class SearchStaxFullIndexJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxFullIndexJobConsumer.class);

    @Reference
    private SearchStaxFullIndexExecutionService executionService;

    @Reference
    private SlingSettingsService slingSettings;

    @Override
    public JobResult process(final Job job) {
        if (!slingSettings.getRunModes().contains("author")) {
            LOG.warn("Full index job skipped on non-author instance");
            return JobResult.CANCEL;
        }

        LOG.info("Processing full index job: id={}", job.getId());
        try {
            executionService.execute(FullIndexPathConfig.fromJob(job));
            final FullIndexProgress.State outcome = executionService.getProgressSnapshot().getState();
            if (outcome == FullIndexProgress.State.FAILED) {
                LOG.error("Full index job catastrophic failure: id={}", job.getId());
                return JobResult.FAILED;
            }
            LOG.info(
                    "Full index job finished: id={}, state={}",
                    job.getId(),
                    outcome);
            return JobResult.OK;
        } catch (final Exception e) {
            LOG.error("Full index job failed: id={}", job.getId(), e);
            return JobResult.FAILED;
        }
    }
}
