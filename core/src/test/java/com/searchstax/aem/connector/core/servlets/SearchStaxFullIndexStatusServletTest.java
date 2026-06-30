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
import org.mockito.Mockito;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private Job job;

    private StringWriter stringWriter;

    @BeforeEach
    void setup() throws Exception {
        stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
        mockNoJobs();
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
    void abbreviateMiddle_truncatesToMaxLengthWhenVerySmall() {
        assertEquals("abc", SearchStaxFullIndexStatusServlet.abbreviateMiddle("abcdef", 3));
    }

    @Test
    void doGet_returnsCompletedSuccessSnapshot() throws Exception {
        final FullIndexProgress progress = sampleProgress(State.SUCCESS, "Full index completed successfully.");
        when(searchStaxFullIndexRunService.getProgress()).thenReturn(progress);

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        verify(response).setStatus(SlingHttpServletResponse.SC_OK);
        assertEquals("SUCCESS", body.get("state").asText());
        assertEquals("Full index completed successfully.", body.get("message").asText());
        assertTrue(body.get("complete").asBoolean());
        assertFalse(body.get("running").asBoolean());
        assertEquals("", body.get("jobId").asText());
        assertEquals(10, body.get("totalProcessed").asLong());
        assertEquals(10, body.get("totalAttempted").asLong());
        assertEquals(1_000L + 500L, body.get("completedAt").asLong());
    }

    @Test
    void doGet_activeJobOverridesIdleSnapshot() throws Exception {
        when(jobManager.findJobs(
                        eq(JobManager.QueryType.ACTIVE),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenReturn(List.of(job));
        when(job.getId()).thenReturn("job-active");
        when(searchStaxFullIndexRunService.getProgress())
                .thenReturn(sampleProgress(State.IDLE, "Idle"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("RUNNING", body.get("state").asText());
        assertEquals("job-active", body.get("jobId").asText());
        assertEquals("Full index job is running.", body.get("message").asText());
        assertTrue(body.get("running").asBoolean());
        assertFalse(body.get("complete").asBoolean());
        assertEquals(0L, body.get("completedAt").asLong());
    }

    @Test
    void doGet_queuedJobUsedWhenNoActiveJob() throws Exception {
        when(jobManager.findJobs(
                        eq(JobManager.QueryType.QUEUED),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenReturn(List.of(job));
        when(job.getId()).thenReturn("job-queued");
        when(searchStaxFullIndexRunService.getProgress())
                .thenReturn(sampleProgress(State.SUCCESS, "Done"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("RUNNING", body.get("state").asText());
        assertEquals("job-queued", body.get("jobId").asText());
        assertTrue(body.get("running").asBoolean());
        assertFalse(body.get("complete").asBoolean());
    }

    @Test
    void doGet_runningSnapshotWithoutJob() throws Exception {
        when(searchStaxFullIndexRunService.getProgress())
                .thenReturn(sampleProgress(State.RUNNING, "Indexing pages"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("RUNNING", body.get("state").asText());
        assertTrue(body.get("running").asBoolean());
        assertFalse(body.get("complete").asBoolean());
        assertEquals("Indexing pages", body.get("message").asText());
    }

    @Test
    void doGet_partialFailureMarkedComplete() throws Exception {
        when(searchStaxFullIndexRunService.getProgress())
                .thenReturn(sampleProgress(State.PARTIAL_FAILURE, "Completed with failures"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("PARTIAL_FAILURE", body.get("state").asText());
        assertTrue(body.get("complete").asBoolean());
        assertFalse(body.get("running").asBoolean());
    }

    @Test
    void doGet_abbreviatesLastIndexedPathInResponse() throws Exception {
        final String longPath =
                "/content/wknd/us/en/magazine/adventures/alaska-adventure/details/"
                        + "very-long-segment-for-ui-truncation-check-extra-path-segments";
        final FullIndexProgress progress =
                new FullIndexProgress(
                        State.SUCCESS,
                        1,
                        1,
                        0,
                        1,
                        0,
                        1,
                        longPath,
                        1_000L,
                        100L,
                        "done");
        when(searchStaxFullIndexRunService.getProgress()).thenReturn(progress);

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        final String abbreviated = body.get("lastIndexedPath").asText();
        assertEquals(120, abbreviated.length());
        assertTrue(abbreviated.contains("..."));
    }

    @Test
    void doGet_handlesNullJobIdInActiveCollection() throws Exception {
        when(jobManager.findJobs(
                        eq(JobManager.QueryType.ACTIVE),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenReturn(Collections.singletonList(null));
        when(searchStaxFullIndexRunService.getProgress())
                .thenReturn(sampleProgress(State.RUNNING, "Still indexing"));

        servlet.doGet(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        assertEquals("", body.get("jobId").asText());
        assertEquals("RUNNING", body.get("state").asText());
    }

    private void mockNoJobs() {
        when(jobManager.findJobs(
                        eq(JobManager.QueryType.ACTIVE),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenReturn(Collections.emptyList());
        when(jobManager.findJobs(
                        eq(JobManager.QueryType.QUEUED),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull()))
                .thenReturn(Collections.emptyList());
    }

    private static FullIndexProgress sampleProgress(final State state, final String message) {
        return new FullIndexProgress(state, 10, 10, 0, 8, 2, 3, "/content/wknd/en/page", 1_000L, 500L, message);
    }
}
