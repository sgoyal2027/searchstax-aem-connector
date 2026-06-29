package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexProgress.State;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchStaxFullIndexStatusServletTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @InjectMocks
    private SearchStaxFullIndexStatusServlet servlet;

    @Mock
    private SearchStaxFullIndexRunService searchStaxFullIndexRunService;

    @Mock
    private JobManager jobManager;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private StringWriter stringWriter;

    @BeforeEach
    void setup() throws Exception {
        stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
        stubNoJobs();
    }

    private void stubNoJobs() {
        when(jobManager.findJobs(
                        any(),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void abbreviateMiddle_returnsOriginalWhenWithinLimit() {
        assertEquals("/content/wknd/en/page", SearchStaxFullIndexStatusServlet.abbreviateMiddle("/content/wknd/en/page", 120));
    }

    @Test
    void abbreviateMiddle_shortensLongPaths() {
        final String path = "/content/wknd/us/en/magazine/adventures/alaska-adventure/details/very-long-segment";
        final String abbreviated = SearchStaxFullIndexStatusServlet.abbreviateMiddle(path, 40);

        assertEquals(40, abbreviated.length());
        assertTrue(abbreviated.contains("..."));
    }

    @Test
    void abbreviateMiddle_handlesNullAndEmpty() {
        assertEquals("", SearchStaxFullIndexStatusServlet.abbreviateMiddle(null, 40));
        assertEquals("", SearchStaxFullIndexStatusServlet.abbreviateMiddle("", 40));
        assertEquals("", SearchStaxFullIndexStatusServlet.abbreviateMiddle(null, 0));
    }

    @Test
    void doGet_reportsCompletedRunWithFrozenElapsed() throws Exception {
        when(searchStaxFullIndexRunService.getProgress()).thenReturn(
                new FullIndexProgress(State.SUCCESS, 10, 10, 0, 8, 2, 1, "/content/wknd/en/page", 1_000L, 5_000L, "Done"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("SUCCESS", body.get("state").asText());
        assertTrue(body.get("complete").asBoolean());
        assertFalse(body.get("running").asBoolean());
        assertEquals(5_000L, body.get("elapsedMs").asLong());
        assertEquals(6_000L, body.get("completedAt").asLong());
    }

    @Test
    void doGet_reportsRunningSnapshotWhileJobActive() throws Exception {
        final Job activeJob = org.mockito.Mockito.mock(Job.class);
        when(activeJob.getId()).thenReturn("job-123");
        when(jobManager.findJobs(
                        any(),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenAnswer(invocation -> {
                    if (invocation.getArgument(0) == JobManager.QueryType.ACTIVE) {
                        return List.of(activeJob);
                    }
                    return Collections.emptyList();
                });
        when(searchStaxFullIndexRunService.getProgress()).thenReturn(
                new FullIndexProgress(State.RUNNING, 3, 3, 0, 2, 1, 1, "/content/wknd/en", 2_000L, 1_500L, "Indexing"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("RUNNING", body.get("state").asText());
        assertEquals("job-123", body.get("jobId").asText());
        assertTrue(body.get("running").asBoolean());
        assertFalse(body.get("complete").asBoolean());
        assertEquals(1_500L, body.get("elapsedMs").asLong());
        assertEquals("Full index job is running.", body.get("message").asText());
    }

    @Test
    void doGet_hidesStaleCountersWhileNewJobQueued() throws Exception {
        final Job queuedJob = org.mockito.Mockito.mock(Job.class);
        when(queuedJob.getId()).thenReturn("job-new");
        when(jobManager.findJobs(
                        any(),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenAnswer(invocation -> {
                    if (invocation.getArgument(0) == JobManager.QueryType.QUEUED) {
                        return List.of(queuedJob);
                    }
                    return Collections.emptyList();
                });
        when(searchStaxFullIndexRunService.getProgress()).thenReturn(
                new FullIndexProgress(State.IDLE, 0, 0, 0, 0, 0, 0, "", 0L, 0L, ""));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("RUNNING", body.get("state").asText());
        assertTrue(body.get("running").asBoolean());
        assertFalse(body.get("complete").asBoolean());
        assertEquals(0L, body.get("totalProcessed").asLong());
        assertEquals(0L, body.get("pagesIndexed").asLong());
        assertEquals(0L, body.get("assetsIndexed").asLong());
        assertEquals(0, body.get("currentBatchNumber").asInt());
        assertEquals(0L, body.get("elapsedMs").asLong());
        assertEquals("", body.get("lastIndexedPath").asText());
    }

    @Test
    void doGet_reportsCompleteDespiteStaleActiveJob() throws Exception {
        final Job activeJob = org.mockito.Mockito.mock(Job.class);
        when(activeJob.getId()).thenReturn("job-stale");
        when(jobManager.findJobs(
                        any(),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenAnswer(invocation -> {
                    if (invocation.getArgument(0) == JobManager.QueryType.ACTIVE) {
                        return List.of(activeJob);
                    }
                    return Collections.emptyList();
                });
        when(searchStaxFullIndexRunService.getProgress()).thenReturn(
                new FullIndexProgress(
                        State.SUCCESS, 40, 40, 0, 9, 31, 1,
                        "/content/dam/wknd-shared/en/magazine/western-australia/adobestock-156407519.jpeg",
                        1_000L, 120_000L, "Full index completed successfully"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("SUCCESS", body.get("state").asText());
        assertTrue(body.get("complete").asBoolean());
        assertFalse(body.get("running").asBoolean());
        assertEquals(40L, body.get("totalProcessed").asLong());
        assertEquals(120_000L, body.get("elapsedMs").asLong());
        assertEquals("Full index completed successfully", body.get("message").asText());
    }

    @Test
    void doGet_hidesElapsedWhileIdle() throws Exception {
        when(searchStaxFullIndexRunService.getProgress()).thenReturn(
                new FullIndexProgress(State.IDLE, 0, 0, 0, 0, 0, 0, "", 0L, 0L, ""));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("IDLE", body.get("state").asText());
        assertEquals(0L, body.get("elapsedMs").asLong());
        assertEquals(0L, body.get("startedAt").asLong());
        assertFalse(body.get("running").asBoolean());
    }
}
