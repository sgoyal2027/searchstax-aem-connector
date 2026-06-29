package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexProgress.State;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexExecutionService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexOrchestratorService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexPathConfigurationService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component(service = SearchStaxFullIndexOrchestratorService.class)
@SuppressWarnings("CQRules:AMSCORE-553")
public class SearchStaxFullIndexOrchestratorServiceImpl
        implements SearchStaxFullIndexOrchestratorService {

    private static final Logger LOG =
            LoggerFactory.getLogger(SearchStaxFullIndexOrchestratorServiceImpl.class);

    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_BAD_REQUEST = 400;

    @Reference
    private JobManager jobManager;

    @Reference
    private SlingSettingsService slingSettings;

    @Reference
    private InitialSetupConfigService initialSetupConfigService;

    @Reference
    private SearchStaxFullIndexExecutionService executionService;

    @Reference
    private SearchStaxFullIndexPathConfigurationService pathConfigurationService;

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public FullIndexTriggerResult triggerFullIndex(final FullIndexPathConfig config) {

        if (!isAuthor()) {
            return new FullIndexTriggerResult(
                    false, "", "Full index can only be started on author.", HTTP_BAD_REQUEST);
        }

        final InitialSetupConfig setupConfig = initialSetupConfigService.getConfiguration();

        LOG.debug("Enable Connector: {}", setupConfig.isEnableConnector());

        if (!setupConfig.isEnableConnector()) {
            LOG.info("SearchStax connector is disabled via Initial Setup configuration. Skipping full index.");
            return new FullIndexTriggerResult(
                    false, "", "SearchStax connector is disabled.", HTTP_BAD_REQUEST);
        }

        if (hasActiveOrQueuedJob()) {
            return new FullIndexTriggerResult(
                    false,
                    "",
                    "A full index job is already running or queued.",
                    HTTP_CONFLICT);
        }

        /*
         * Resolve effective includes (source of truth)
         */
        final String[] effectiveIncludes =
                pathConfigurationService.resolveEffectiveIncludes(config);

        /*
         * ==============================
         * INPUTS (declare ONCE)
         * ==============================
         */
        final String root = config.getRootPath();
        final String[] includes = config.getIncludePaths();
        final String[] excludes = config.getExcludePaths();
        if (root == null || root.trim().isEmpty()) {
            LOG.warn("Root path is missing");
            return new FullIndexTriggerResult(
                    false, "", "Root Path is required to start full indexing.", HTTP_BAD_REQUEST);
        }

        if (!pathExists(root)) {
            LOG.warn("Root path does not exist in JCR: {}", root);
            return new FullIndexTriggerResult(
                    false, "", "Root path does not exist: " + root, HTTP_BAD_REQUEST);
        }

        /*
         * ==============================
         * INCLUDE VALIDATION (STRICT)
         * ==============================
         */
        if (includes != null && includes.length > 0) {
            for (String include : includes) {
                if (include == null || include.trim().isEmpty()) {
                    continue;
                }
                boolean underRoot = include.equals(root) || include.startsWith(root + "/");
                if (!underRoot) {
                    LOG.warn("Invalid include path {} not under root {}", include, root);
                    return new FullIndexTriggerResult(
                            false, "", "All include paths must be under root path.", HTTP_BAD_REQUEST);
                }
                if (!pathExists(include)) {
                    LOG.warn("Include path does not exist in JCR: {}", include);
                    return new FullIndexTriggerResult(
                            false, "", "Include path does not exist: " + include, HTTP_BAD_REQUEST);
                }
            }
        }

        /*
         * ==============================
         * EXCLUDE VALIDATION (SAFE)
         * ==============================
         */
        if (excludes != null && excludes.length > 0) {

            for (String exclude : excludes) {

                if (exclude == null || exclude.trim().isEmpty()) {
                    continue;
                }

                boolean underRoot =
                        root != null &&
                        (exclude.equals(root) || exclude.startsWith(root + "/"));

                boolean underInclude = false;

                if (includes != null) {
                    for (String include : includes) {
                        if (include != null
                                && !include.trim().isEmpty()
                                && (exclude.equals(include)
                                || exclude.startsWith(include + "/"))) {
                            underInclude = true;
                            break;
                        }
                    }
                }

                if (!underRoot && !underInclude) {

                    LOG.warn("Invalid exclude path {} not under root or includes", exclude);

                    return new FullIndexTriggerResult(
                            false,
                            "",
                            "Exclude paths must be under root or include paths.",
                            HTTP_BAD_REQUEST);
                }
            }
        }

        /*
         * Final safety check from resolver
         */
        if (effectiveIncludes.length == 0) {

            LOG.warn("No valid include paths after resolution for root {}", root);

            return new FullIndexTriggerResult(
                    false,
                    "",
                    "No valid include paths under the configured root path.",
                    HTTP_BAD_REQUEST);
        }

        /*
         * Create job
         */
        final Map<String, Object> props =
                new HashMap<>(config.toJobProperties());

        props.put("triggeredAt", System.currentTimeMillis());
        props.put(Job.PROPERTY_JOB_RETRIES, 0);

        LOG.info(
                "Full index config root={}, includes={}, excludes={}, includeChildPaths={}",
                root,
                effectiveIncludes,
                excludes,
                Arrays.toString(config.getIncludeChildPaths()));

        final Job job =
                jobManager.addJob(SearchStaxFullIndexDefaults.JOB_TOPIC, props);

        if (job == null) {
            return new FullIndexTriggerResult(
                    false,
                    "",
                    "Could not queue full index job.",
                    HTTP_BAD_REQUEST);
        }

        executionService.clearProgressForNewRun();

        return new FullIndexTriggerResult(
                true,
                job.getId(),
                "Full index started in background.",
                HTTP_ACCEPTED);
    }

    @Override
    public FullIndexProgress getProgress() {
        return executionService.getProgressSnapshot();
    }

    private boolean isAuthor() {
        return slingSettings.getRunModes().contains("author");
    }

    private boolean pathExists(final String path) {
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            return resolver.getResource(path) != null;
        } catch (final LoginException e) {
            LOG.warn("Could not verify path existence for {}", path, e);
            return false;
        }
    }

    private boolean hasActiveOrQueuedJob() {
        if (!hasJobsInActiveOrQueuedQueues()) {
            return false;
        }
        final State progressState = executionService.getProgressSnapshot().getState();
        return progressState != State.SUCCESS
                && progressState != State.PARTIAL_FAILURE
                && progressState != State.FAILED;
    }

    private boolean hasJobsInActiveOrQueuedQueues() {

        final Collection<Job> active =
                jobManager.findJobs(
                        JobManager.QueryType.ACTIVE,
                        SearchStaxFullIndexDefaults.JOB_TOPIC,
                        -1,
                        (Map<String, Object>[]) null);

        if (active != null && !active.isEmpty()) {
            return true;
        }

        final Collection<Job> queued =
                jobManager.findJobs(
                        JobManager.QueryType.QUEUED,
                        SearchStaxFullIndexDefaults.JOB_TOPIC,
                        -1,
                        (Map<String, Object>[]) null);

        return queued != null && !queued.isEmpty();
    }
}