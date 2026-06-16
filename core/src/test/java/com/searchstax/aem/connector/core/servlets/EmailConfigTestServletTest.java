package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailConfigTestServletTest {

    @Test
    void buildTestConfig_usesPostedValuesWhenPresent() {
        final SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getParameter("smtpHost")).thenReturn("smtp.example.com");
        when(request.getParameter("smtpPort")).thenReturn("587");
        when(request.getParameter("smtpUser")).thenReturn("user@example.com");
        when(request.getParameter("fromEmail")).thenReturn("from@example.com");
        when(request.getParameter("receiverEmails")).thenReturn("ops@example.com");
        when(request.getParameter("smtpUseSSL")).thenReturn("false");
        when(request.getParameter("smtpUseStartTLS")).thenReturn("true");
        when(request.getParameter("smtpPassword")).thenReturn("secret");

        final EmailConfig saved = new EmailConfig();
        saved.setSmtpHost("old.example.com");
        saved.setSmtpPort(25);
        saved.setSmtpPassword("{protected}");

        final EmailConfig config = EmailConfigTestServlet.buildTestConfig(request, saved);

        assertEquals("smtp.example.com", config.getSmtpHost());
        assertEquals(587, config.getSmtpPort());
        assertEquals("user@example.com", config.getSmtpUser());
        assertEquals("from@example.com", config.getFromEmail());
        assertEquals("ops@example.com", config.getReceiverEmails());
        assertFalse(config.isSmtpUseSsl());
        assertTrue(config.isSmtpUseStartTls());
        assertEquals("secret", config.getSmtpPassword());
    }

    @Test
    void buildTestConfig_fallsBackToSavedPasswordWhenPostedBlank() {
        final SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getParameter("smtpHost")).thenReturn("smtp.example.com");
        when(request.getParameter("smtpPort")).thenReturn("587");
        when(request.getParameter("receiverEmails")).thenReturn("ops@example.com");
        when(request.getParameter("smtpPassword")).thenReturn("");

        final EmailConfig saved = new EmailConfig();
        saved.setSmtpPassword("{protected-password}");

        final EmailConfig config = EmailConfigTestServlet.buildTestConfig(request, saved);

        assertEquals("{protected-password}", config.getSmtpPassword());
    }
}
