package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingReportClearServletTest {

    private static final Gson GSON = new Gson();
    private static final Type PAYLOAD_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private FullIndexAuditService fullIndexAuditService;

    @Mock
    private SearchStaxFullIndexFailureStore fullIndexFailureStore;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private final StringWriter responseBody = new StringWriter();
    private final IndexingReportClearServlet servlet = new IndexingReportClearServlet();

    @BeforeEach
    void setUp() throws Exception {
        OsgiFieldInjector.inject(servlet, "indexingAuditService", indexingAuditService);
        OsgiFieldInjector.inject(servlet, "fullIndexAuditService", fullIndexAuditService);
        OsgiFieldInjector.inject(servlet, "fullIndexFailureStore", fullIndexFailureStore);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    void doPost_incrementalReport_clearsAuditEvents() throws Exception {
        when(request.getParameter("type")).thenReturn("incremental");
        when(indexingAuditService.clearAllEvents()).thenReturn(42);

        servlet.doPost(request, response);

        verify(indexingAuditService).clearAllEvents();
        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        assertTrue((Boolean) payload.get("success"));
        assertEquals("incremental", payload.get("type"));
        assertEquals(42, ((Number) payload.get("removedCount")).intValue());
    }

    @Test
    void doPost_fullReport_clearsSuccessAndFailureEvents() throws Exception {
        when(request.getParameter("type")).thenReturn("full");
        when(fullIndexAuditService.clearAllEvents()).thenReturn(10);
        when(fullIndexFailureStore.clearAllFailures()).thenReturn(5);

        servlet.doPost(request, response);

        verify(fullIndexAuditService).clearAllEvents();
        verify(fullIndexFailureStore).clearAllFailures();
        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        assertEquals("full", payload.get("type"));
        assertEquals(15, ((Number) payload.get("removedCount")).intValue());
    }

    @Test
    void doPost_fullReport_includesSuccessAndFailureBreakdown() throws Exception {
        when(request.getParameter("type")).thenReturn("full");
        when(fullIndexAuditService.clearAllEvents()).thenReturn(7);
        when(fullIndexFailureStore.clearAllFailures()).thenReturn(3);

        servlet.doPost(request, response);

        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        assertEquals(7, ((Number) payload.get("removedSuccessEvents")).intValue());
        assertEquals(3, ((Number) payload.get("removedFailureEvents")).intValue());
    }

    @Test
    void doPost_returnsInternalErrorWhenClearFails() throws Exception {
        when(request.getParameter("type")).thenReturn("incremental");
        when(indexingAuditService.clearAllEvents()).thenThrow(new RuntimeException("JCR error"));

        servlet.doPost(request, response);

        verify(response).setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseBody.toString().contains("\"success\":false"));
        assertTrue(responseBody.toString().contains("Unable to clear indexing report"));
    }
}
