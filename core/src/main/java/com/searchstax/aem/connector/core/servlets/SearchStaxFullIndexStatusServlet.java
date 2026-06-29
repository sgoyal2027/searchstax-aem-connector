package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.wizard.SearchStaxWizardBindingPaths;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexProgress.State;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
            Constants.SERVICE_DESCRIPTION + "=SearchStax full index status",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
            ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SearchStaxWizardBindingPaths.SERVLET_FULL_INDEX_STATUS,
            Constants.SERVICE_RANKING + ":Integer=200000"
        })
public class SearchStaxFullIndexStatusServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final int ABBREVIATED_PATH_LENGTH = 120;

    @Reference
    private transient SearchStaxFullIndexRunService searchStaxFullIndexRunService;

    @Reference
    private transient JobManager jobManager;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final FullIndexProgress progress = searchStaxFullIndexRunService.getProgress();
        final String activeJobId = firstJobId(JobManager.QueryType.ACTIVE);
        final String queuedJobId = firstJobId(JobManager.QueryType.QUEUED);
        final boolean hasActiveOrQueuedJob = !activeJobId.isEmpty() || !queuedJobId.isEmpty();
        final String currentJobId = !activeJobId.isEmpty() ? activeJobId : queuedJobId;

        final State snapshotState = progress.getState();
        final State state = hasActiveOrQueuedJob ? State.RUNNING : snapshotState;
        final boolean running = hasActiveOrQueuedJob || snapshotState == State.RUNNING;
        final boolean complete =
                !hasActiveOrQueuedJob
                        && (snapshotState == State.SUCCESS
                        || snapshotState == State.PARTIAL_FAILURE
                        || snapshotState == State.FAILED);
        final boolean currentRunInProgress = snapshotState == State.RUNNING;
        final long startedAt = currentRunInProgress ? progress.getStartedAt() : 0L;
        final long elapsedMs = currentRunInProgress ? progress.getElapsedMs() : 0L;

        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", currentJobId);
        body.put("state", state.name());
        body.put("message", hasActiveOrQueuedJob ? "Full index job is running." : progress.getMessage());
        body.put("totalProcessed", progress.getTotalProcessed());
        body.put("successCount", progress.getSuccessCount());
        body.put("failureCount", progress.getFailureCount());
        body.put("totalAttempted", progress.getTotalAttempted());
        body.put("pagesIndexed", progress.getPagesIndexed());
        body.put("assetsIndexed", progress.getAssetsIndexed());
        body.put("currentBatchNumber", progress.getCurrentBatchNumber());
        body.put("lastIndexedPath", abbreviateMiddle(progress.getLastIndexedPath(), ABBREVIATED_PATH_LENGTH));
        body.put("startedAt", startedAt);
        body.put("elapsedMs", complete ? progress.getElapsedMs() : elapsedMs);
        body.put(
                "completedAt",
                complete && progress.getStartedAt() > 0
                        ? progress.getStartedAt() + progress.getElapsedMs()
                        : 0L);
        body.put("running", running);
        body.put("complete", complete);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setStatus(SlingHttpServletResponse.SC_OK);
        new ObjectMapper().writeValue(response.getWriter(), body);
    }

    private String firstJobId(final JobManager.QueryType queryType) {
        final Collection<Job> jobs =
                jobManager.findJobs(queryType, SearchStaxFullIndexDefaults.JOB_TOPIC, -1, (Map<String, Object>[]) null);
        if (jobs == null || jobs.isEmpty()) {
            return "";
        }
        final Job first = jobs.iterator().next();
        return first == null || first.getId() == null ? "" : first.getId();
    }

    static String abbreviateMiddle(final String value, final int maxLength) {
        if (value == null || value.isEmpty() || maxLength <= 0 || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        final String marker = "...";
        final int charsToKeep = maxLength - marker.length();
        final int front = charsToKeep / 2;
        final int back = charsToKeep - front;
        return value.substring(0, front) + marker + value.substring(value.length() - back);
    }
}
