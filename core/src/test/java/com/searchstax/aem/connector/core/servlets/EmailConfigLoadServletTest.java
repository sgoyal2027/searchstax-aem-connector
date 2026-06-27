package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfigLoadServletTest {

    @InjectMocks
    private EmailConfigLoadServlet servlet;

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {

        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(writer);
    }

    private EmailConfig createConfig() {

        EmailConfig config = new EmailConfig();

        config.setSmtpHost("smtp.gmail.com");
        config.setSmtpPort(587);
        config.setSmtpUser("user@gmail.com");
        config.setSmtpPassword("ENC(password)");
        config.setFromEmail("from@gmail.com");
        config.setReceiverEmails("test@test.com");
        config.setSmtpUseSsl(true);
        config.setSmtpUseStartTls(true);
        config.setNotifyOnIndexingFailure(true);

        return config;
    }

    @Test
    void testDoGetSuccess() throws Exception {

        when(emailConfigService.getConfiguration())
                .thenReturn(createConfig());

        servlet.doGet(request, response);

        String json = stringWriter.toString();

        assertTrue(json.contains("\"smtpHost\":\"smtp.gmail.com\""));
        assertTrue(json.contains("\"smtpPort\":587"));
        assertTrue(json.contains("\"smtpUser\":\"user@gmail.com\""));
        assertTrue(json.contains("\"smtpPassword\":\"\""));
        assertTrue(json.contains("\"fromEmail\":\"from@gmail.com\""));
        assertTrue(json.contains("\"receiverEmails\":\"test@test.com\""));
        assertTrue(json.contains("\"smtpUseSSL\":true"));
        assertTrue(json.contains("\"smtpUseStartTLS\":true"));
        assertTrue(json.contains("\"notifyOnIndexingFailure\":true"));
        assertTrue(json.contains("\"hasSavedPassword\":true"));
        assertTrue(json.contains("\"configurationSaved\":true"));

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    void testDefaultPortWhenZero() throws Exception {

        EmailConfig config = createConfig();
        config.setSmtpPort(0);

        when(emailConfigService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        assertTrue(stringWriter.toString().contains("\"smtpPort\":25"));
    }

    @Test
    void testNullValues() throws Exception {

        EmailConfig config = new EmailConfig();

        when(emailConfigService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        String json = stringWriter.toString();

        assertTrue(json.contains("\"smtpHost\":\"\""));
        assertTrue(json.contains("\"smtpUser\":\"\""));
        assertTrue(json.contains("\"smtpPassword\":\"\""));
        assertTrue(json.contains("\"fromEmail\":\"\""));
        assertTrue(json.contains("\"receiverEmails\":\"\""));
        assertTrue(json.contains("\"hasSavedPassword\":false"));
        assertTrue(json.contains("\"configurationSaved\":false"));
    }

    @Test
    void testConfigurationSavedFalseMissingHost() throws Exception {

        EmailConfig config = createConfig();
        config.setSmtpHost("");

        when(emailConfigService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        assertTrue(stringWriter.toString()
                .contains("\"configurationSaved\":false"));
    }

    @Test
    void testConfigurationSavedFalseMissingReceiver() throws Exception {

        EmailConfig config = createConfig();
        config.setReceiverEmails("");

        when(emailConfigService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        assertTrue(stringWriter.toString()
                .contains("\"configurationSaved\":false"));
    }

    @Test
    void testConfigurationSavedFalseMissingPassword() throws Exception {

        EmailConfig config = createConfig();
        config.setSmtpPassword("");

        when(emailConfigService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        String json = stringWriter.toString();

        assertTrue(json.contains("\"hasSavedPassword\":false"));
        assertTrue(json.contains("\"configurationSaved\":false"));
    }

    @Test
    void testConfigurationSavedTrue() throws Exception {

        when(emailConfigService.getConfiguration())
                .thenReturn(createConfig());

        servlet.doGet(request, response);

        assertTrue(stringWriter.toString()
                .contains("\"configurationSaved\":true"));
    }

    @Test
    void testException() throws Exception {

        when(emailConfigService.getConfiguration())
                .thenThrow(new RuntimeException("Failure"));

        servlet.doGet(request, response);

        verify(response)
                .setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        assertTrue(stringWriter.toString()
                .contains("Unable to load configuration"));
    }
}