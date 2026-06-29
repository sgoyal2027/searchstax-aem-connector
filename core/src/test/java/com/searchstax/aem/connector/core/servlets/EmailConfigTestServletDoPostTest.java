package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.services.EmailService;
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

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailConfigTestServletDoPostTest {

    private static final Gson GSON = new Gson();
    private static final Type PAYLOAD_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private EmailService emailService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private final StringWriter responseBody = new StringWriter();
    private final EmailConfigTestServlet servlet = new EmailConfigTestServlet();

    @BeforeEach
    void setUp() throws Exception {
        OsgiFieldInjector.inject(servlet, "emailConfigService", emailConfigService);
        OsgiFieldInjector.inject(servlet, "emailService", emailService);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    void doPost_missingReceivers_returnsBadRequest() throws Exception {
        final EmailConfig saved = new EmailConfig();
        saved.setReceiverEmails("");
        when(emailConfigService.getConfiguration()).thenReturn(saved);
        when(request.getParameter("receiverEmails")).thenReturn("");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseBody.toString().contains("receiver email address"));
    }

    @Test
    void doPost_sendSucceeds_returnsSuccessPayload() throws Exception {
        final EmailConfig saved = savedConfig();
        when(emailConfigService.getConfiguration()).thenReturn(saved);
        stubPostedParameters();
        when(emailService.sendEmailOrError(any(), any(EmailConfig.class))).thenReturn(null);

        servlet.doPost(request, response);

        verify(emailService).sendEmailOrError(any(), any(EmailConfig.class));
        final Map<String, Object> payload = GSON.fromJson(responseBody.toString(), PAYLOAD_TYPE);
        assertTrue((Boolean) payload.get("success"));
        assertTrue(payload.get("message").toString().contains("Test email sent"));
    }

    @Test
    void doPost_sendFails_returnsInternalError() throws Exception {
        final EmailConfig saved = savedConfig();
        when(emailConfigService.getConfiguration()).thenReturn(saved);
        stubPostedParameters();
        when(emailService.sendEmailOrError(any(), any(EmailConfig.class)))
                .thenReturn("SMTP authentication failed");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseBody.toString().contains("SMTP authentication failed"));
        assertTrue(responseBody.toString().contains("searchstaxconnector.log"));
    }

    private void stubPostedParameters() {
        when(request.getParameter("smtpHost")).thenReturn("smtp.example.com");
        when(request.getParameter("smtpPort")).thenReturn("587");
        when(request.getParameter("receiverEmails")).thenReturn("ops@example.com, alerts@example.com");
    }

    private static EmailConfig savedConfig() {
        final EmailConfig saved = new EmailConfig();
        saved.setSmtpHost("smtp.example.com");
        saved.setSmtpPort(587);
        saved.setReceiverEmails("ops@example.com");
        return saved;
    }
}
