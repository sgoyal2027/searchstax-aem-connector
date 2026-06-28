package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.searchstax.aem.connector.core.dto.IndexingReportPage;
import com.searchstax.aem.connector.core.services.FullIndexAuditService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexFailureStore;
import com.searchstax.aem.connector.core.testutil.OsgiFieldInjector;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndexingReportServletTest {

    private static final Gson GSON = new Gson();
    private static final Type PAYLOAD_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private SearchStaxFullIndexFailureStore fullIndexFailureStore;

    @Mock
    private FullIndexAuditService fullIndexAuditService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private final StringWriter responseBody = new StringWriter();
    private final IndexingReportServlet servlet = new IndexingReportServlet();

    @BeforeEach
    void setUp() throws Exception {
        OsgiFieldInjector.inject(servlet, "indexingAuditService", indexingAuditService);
        OsgiFieldInjector.inject(servlet, "fullIndexFailureStore", fullIndexFailureStore);
        OsgiFieldInjector.inject(servlet, "fullIndexAuditService", fullIndexAuditService);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    void doGet_incrementalReport_delegatesToAuditService() throws Exception {
        final List<Map<String, Object>> events = List.of(eventRow("SUCCESS", "Indexed"));
        when(request.getParameter("type")).thenReturn("incremental");
        when(request.getParameter("status")).thenReturn("SUCCESS");
        when(request.getParameter("action")).thenReturn("ACTIVATE");
        when(request.getParameter("excludeQueued")).thenReturn("true");
        when(request.getParameter("page")).thenReturn("2");
        when(request.getParameter("pageSize")).thenReturn("100");
        when(indexingAuditService.listEventsPaged("SUCCESS", "ACTIVATE", true, 100, 100))
                .thenReturn(new IndexingReportPage(events, 250));

        servlet.doGet(request, response);

        verify(indexingAuditService).listEventsPaged("SUCCESS", "ACTIVATE", true, 100, 100);
        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        assertEquals(true, payload.get("success"));
        assertEquals("incremental", payload.get("type"));
        assertEquals(1, ((List<?>) payload.get("events")).size());
        assertEquals(2, ((Number) payload.get("page")).intValue());
        assertEquals(100, ((Number) payload.get("pageSize")).intValue());
        assertEquals(250, ((Number) payload.get("totalCount")).intValue());
        assertEquals(3, ((Number) payload.get("totalPages")).intValue());
    }

    @Test
    void doGet_fullReport_mergesSuccessAndFailureEvents() throws Exception {
        when(request.getParameter("type")).thenReturn("full");
        when(request.getParameter("page")).thenReturn("1");
        when(request.getParameter("pageSize")).thenReturn("100");
        when(request.getParameter("retentionHours")).thenReturn("24");
        when(fullIndexAuditService.listEventsForReport(null, 10000, 24))
                .thenReturn(List.of(eventRow("SUCCESS", "Successfully posted")));
        when(fullIndexFailureStore.listFailureEventsForReport(null, 10000, 24))
                .thenReturn(List.of(failureRow("2026-06-18 10:00:00", "PATH")));

        servlet.doGet(request, response);

        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        assertEquals("full", payload.get("type"));
        assertEquals(2, ((List<?>) payload.get("events")).size());
        assertEquals(2, ((Number) payload.get("totalCount")).intValue());
        assertEquals(1, ((Number) payload.get("totalPages")).intValue());
    }

    @Test
    void doGet_fullReport_filtersBatchFailuresIncludingSuccessRows() throws Exception {
        when(request.getParameter("type")).thenReturn("full");
        when(request.getParameter("failureKind")).thenReturn("BATCH");
        when(request.getParameter("page")).thenReturn("1");
        when(request.getParameter("pageSize")).thenReturn("100");
        when(request.getParameter("retentionHours")).thenReturn("24");
        when(fullIndexAuditService.listEventsForReport(null, 10000, 24))
                .thenReturn(List.of(eventRow("SUCCESS", "ok")));
        when(fullIndexFailureStore.listFailureEventsForReport(null, 10000, 24))
                .thenReturn(List.of(
                        failureRow("2026-06-18 11:00:00", "PATH"),
                        failureRow("2026-06-18 10:00:00", "BATCH")));

        servlet.doGet(request, response);

        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        final List<Map<String, Object>> events = GSON.fromJson(GSON.toJson(payload.get("events")), LIST_MAP_TYPE);
        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(event -> "SUCCESS".equals(event.get("status"))));
        assertTrue(events.stream().anyMatch(event -> "BATCH".equals(event.get("failureKind"))));
    }

    @Test
    void doGet_fullReport_filtersPathFailuresOnly() throws Exception {
        when(request.getParameter("type")).thenReturn("full");
        when(request.getParameter("failureKind")).thenReturn("PATH");
        when(request.getParameter("page")).thenReturn("1");
        when(request.getParameter("pageSize")).thenReturn("100");
        when(request.getParameter("retentionHours")).thenReturn("24");
        when(fullIndexAuditService.listEventsForReport(null, 10000, 24))
                .thenReturn(List.of(eventRow("SUCCESS", "ok")));
        when(fullIndexFailureStore.listFailureEventsForReport(null, 10000, 24))
                .thenReturn(List.of(
                        failureRow("2026-06-18 11:00:00", "PATH"),
                        failureRow("2026-06-18 10:00:00", "BATCH")));

        servlet.doGet(request, response);

        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        final List<Map<String, Object>> events = GSON.fromJson(GSON.toJson(payload.get("events")), LIST_MAP_TYPE);
        assertEquals(1, events.size());
        assertEquals("PATH", events.get(0).get("failureKind"));
    }

    @Test
    void doGet_fullReport_sortsByTimestampDescending() throws Exception {
        when(request.getParameter("type")).thenReturn("full");
        when(request.getParameter("limit")).thenReturn("50");
        when(request.getParameter("retentionHours")).thenReturn("24");
        when(fullIndexAuditService.listEventsForReport(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(fullIndexFailureStore.listFailureEventsForReport(any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        failureRow("2026-06-18 09:00:00", "BATCH"),
                        failureRow("2026-06-18 12:00:00", "BATCH")));

        servlet.doGet(request, response);

        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        final List<Map<String, Object>> events = GSON.fromJson(GSON.toJson(payload.get("events")), LIST_MAP_TYPE);
        assertEquals("2026-06-18 12:00:00", events.get(0).get("timestamp"));
        assertEquals("2026-06-18 09:00:00", events.get(1).get("timestamp"));
    }

    private static final Type LIST_MAP_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

    private static Map<String, Object> eventRow(final String status, final String message) {
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("timestamp", "2026-06-18 10:00:00");
        row.put("path", "/content/wknd/en/page");
        row.put("status", status);
        row.put("message", message);
        return row;
    }

    private static Map<String, Object> failureRow(final String timestamp, final String failureKind) {
        final Map<String, Object> row = eventRow("FAILURE", "failed");
        row.put("timestamp", timestamp);
        row.put("failureKind", failureKind);
        return row;
    }
}
