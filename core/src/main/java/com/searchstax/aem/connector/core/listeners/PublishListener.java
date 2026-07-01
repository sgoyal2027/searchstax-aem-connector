package com.searchstax.aem.connector.core.listeners;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.config.FullIndexConfigService;
import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
        service = EventHandler.class,
        immediate = true,
        property = {
                "event.topics=" + ReplicationAction.EVENT_TOPIC,
        }
)
public class PublishListener implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PublishListener.class);

    private static final String JOB_TOPIC_INCREMENTAL_INDEX  =
            "searchstaxconnector/incremental-index";

    @Reference
    private JobManager jobManager;

    @Reference
    private SlingSettingsService slingSettings;

    @Reference
    private FullIndexConfigService fullIndexConfigService;

    @Activate
    protected void activate() {
        LOG.info("PublishListener activated successfully");
    }

    @Override
    public void handleEvent(Event event) {

        if (!slingSettings.getRunModes().contains("author")) {
            return;
        }

        ReplicationAction action =
                ReplicationAction.fromEvent(event);

        if (action == null) {
            return;
        }

        ReplicationActionType actionType =
                action.getType();

        LOG.debug("Received replication event | Path={} | Action={}"
                , action.getPath()
                , actionType);

        if (actionType != ReplicationActionType.ACTIVATE
                && actionType != ReplicationActionType.DEACTIVATE
                && actionType != ReplicationActionType.DELETE) {
            return;
        }

        String path = action.getPath();

        if (path == null || path.isBlank()) {

            LOG.warn("Replication path is null or empty");
            return;
        }

        // Skip tags
        if (path.startsWith("/content/cq:tags")) {
            LOG.debug("Skipping tag replication event: {}", path);
            return;
        }

        if (!(path.startsWith("/content")
                || path.startsWith("/content/dam"))) {

            LOG.debug("Skipping unsupported replication path: {}", path);
            return;
        }

        FullIndexConfig config =
                fullIndexConfigService.getConfiguration();

        LOG.debug("Enable Connector: {}", config.isEnableConnector());

        if (!config.isEnableConnector()) {

            LOG.info(
                    "SearchStax connector is disabled via Indexing configuration. Skipping indexing job for path: {}",
                    path);

            return;
        }

        // Root Path Validation
        String[] rootPaths = config.getRootPaths();

        boolean matchesRootPath = false;

        for (String rootPath : rootPaths) {

                if (rootPath != null
                        && !rootPath.isBlank()
                        && path.startsWith(rootPath)) {
                
                matchesRootPath = true;
                break;
                }
        }

        if (rootPaths != null
                && rootPaths.length > 0
                && !matchesRootPath) {

                LOG.debug(
                        "Skipping path {} because it is outside configured root paths",
                        path);

                return;
                }
        

        // Exclude Path Validation
        String[] excludePaths = config.getExcludePaths();

        if (excludePaths != null) {

        for (String excludePath : excludePaths) {

                if (excludePath != null
                && !excludePath.isBlank()
                && path.startsWith(excludePath)) {

                LOG.debug(
                        "Skipping path {} because it matches excluded path {}",
                        path,
                        excludePath);

                return;
                }
        }
        }

        Map<String, Object> jobProperties =
                new HashMap<>();

        jobProperties.put("path", path);

        jobProperties.put("actionType",
                actionType.name());

        jobProperties.put(
                "eventTime",
                System.currentTimeMillis()
        );

        Job job =
                jobManager.addJob(
                        JOB_TOPIC_INCREMENTAL_INDEX ,
                        jobProperties);

        if (job != null) {

            LOG.info("Sling Job created successfully | Path: {} | Action: {} ",
                    path,
                    actionType);

        } else {

            LOG.error("Failed to create Sling Job | Path: {} | Action:{}",
                    path,
                    actionType);
        }
    }
}
