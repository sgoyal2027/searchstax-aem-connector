package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchStaxFullIndexRunServletTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @InjectMocks
    private SearchStaxFullIndexRunServlet servlet;

    @Mock
    private SearchStaxFullIndexRunService searchStaxFullIndexRunService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private final Map<String, String> parameters = new HashMap<>();
    private StringWriter stringWriter;

    @BeforeEach
    void setup() throws Exception {
        stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(request.getParameter(anyString())).thenAnswer(invocation ->
                parameters.get(invocation.getArgument(0)));
        when(request.getParameterValues(anyString())).thenReturn(null);
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
    }

    @Test
    void doPost_returnsAcceptedWhenJobQueued() throws Exception {
        parameters.put("rootPath", "/content/wknd");
        when(searchStaxFullIndexRunService.triggerFullIndex(any()))
                .thenReturn(new FullIndexTriggerResult(true, "job-42", "Full index queued", 202));

        servlet.doPost(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        verify(response).setStatus(202);
        assertTrue(body.get("accepted").asBoolean());
        assertTrue(body.get("success").asBoolean());
        assertEquals("job-42", body.get("jobId").asText());
    }

    @Test
    void doPost_returnsConflictWhenJobAlreadyRunning() throws Exception {
        when(searchStaxFullIndexRunService.triggerFullIndex(any()))
                .thenReturn(new FullIndexTriggerResult(false, "", "A full index job is already running", 409));

        servlet.doPost(request, response);

        final JsonNode body = MAPPER.readTree(stringWriter.toString());
        verify(response).setStatus(409);
        assertFalse(body.get("accepted").asBoolean());
        assertEquals("A full index job is already running", body.get("message").asText());
    }
}
