package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonServletResponseUtilTest {

    @Mock
    private SlingHttpServletResponse response;

    @Test
    void writeSuccess_writesJsonPayload() throws Exception {
        final StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        JsonServletResponseUtil.writeSuccess(response, "Saved");

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        assertTrue(body.toString().contains("\"success\":true"));
        assertTrue(body.toString().contains("Saved"));
    }

    @Test
    void writeBadRequest_setsStatusAndMessage() throws Exception {
        final StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        JsonServletResponseUtil.writeBadRequest(response, "Missing field");

        final ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(response).setStatus(statusCaptor.capture());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, statusCaptor.getValue());
        assertTrue(body.toString().contains("Missing field"));
    }

    @Test
    void escapeJson_escapesQuotesAndBackslashes() {
        assertEquals("line1line2 \\\"quoted\\\"", JsonServletResponseUtil.escapeJson("line1\nline2 \"quoted\""));
    }
}
