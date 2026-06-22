package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingReportReprocessServletTest {

    private static final Gson GSON = new Gson();
    private static final Type PAYLOAD_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private final StringWriter responseBody = new StringWriter();
    private final IndexingReportReprocessServlet servlet = new IndexingReportReprocessServlet();

    @BeforeEach
    void setUp() throws Exception {
        OsgiFieldInjector.inject(servlet, "indexingAuditService", indexingAuditService);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    void doPost_missingPath_returnsBadRequest() throws Exception {
        when(request.getParameter("path")).thenReturn(" ");

        servlet.doPost(request, response);

        verify(response).setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        verify(indexingAuditService, never()).reprocessFailedPath(org.mockito.ArgumentMatchers.anyString());
        assertTrue(responseBody.toString().contains("\"success\":false"));
    }

    @Test
    void doPost_validPath_reprocessesAndReturnsSuccess() throws Exception {
        when(request.getParameter("path")).thenReturn("/content/wknd/en/page");

        servlet.doPost(request, response);

        verify(indexingAuditService).reprocessFailedPath("/content/wknd/en/page");
        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        assertTrue((Boolean) payload.get("success"));
        assertEquals("/content/wknd/en/page", payload.get("path"));
        assertFalse(responseBody.toString().isBlank());
    }
}
