package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @InjectMocks
    private EmailServiceImpl emailService;

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    @Test
    void sendEmailOrError_returnsErrorWhenRequestIsNull() {
        assertEquals(
                "At least one receiver email address is required.",
                emailService.sendEmailOrError(null, validConfig()));
    }

    @Test
    void sendEmailOrError_returnsErrorWhenRecipientsMissing() {
        final EmailRequest request = new EmailRequest();
        request.setRecipients(new String[]{});

        assertEquals(
                "At least one receiver email address is required.",
                emailService.sendEmailOrError(request, validConfig()));
    }

    @Test
    void sendEmailOrError_returnsErrorWhenSmtpNotConfigured() {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost("");
        config.setSmtpPort(0);

        assertEquals(
                "SMTP host and port are required.",
                emailService.sendEmailOrError(emailRequest(), config));
    }

    @Test
    void sendEmailOrError_returnsErrorWhenPasswordMissingForAuthenticatedSmtp() {
        final EmailConfig config = validConfig();
        config.setSmtpUser("user@example.com");
        config.setSmtpPassword("{protected}");

        when(protectedValueCodec.unprotectIfNeeded("{protected}")).thenReturn("");
        when(protectedValueCodec.looksEncrypted("")).thenReturn(false);

        final String error = emailService.sendEmailOrError(emailRequest(), config);

        assertTrue(error.contains("SMTP password is missing or could not be decrypted"));
    }

    @Test
    void sendEmailOrError_returnsErrorWhenPasswordRemainsEncrypted() {
        final EmailConfig config = validConfig();
        config.setSmtpUser("user@example.com");
        config.setSmtpPassword("{protected}");

        when(protectedValueCodec.unprotectIfNeeded("{protected}")).thenReturn("{still-encrypted}");
        when(protectedValueCodec.looksEncrypted("{still-encrypted}")).thenReturn(true);

        final String error = emailService.sendEmailOrError(emailRequest(), config);

        assertTrue(error.contains("SMTP password is missing or could not be decrypted"));
    }

    @Test
    void sendEmail_returnsFalseWhenValidationFails() {
        when(emailConfigService.getConfiguration()).thenReturn(new EmailConfig());

        assertFalse(emailService.sendEmail(emailRequest()));
    }

    @Test
    void sendEmailOrError_usesEmailConfigServiceWhenConfigNotProvided() {
        when(emailConfigService.getConfiguration()).thenReturn(new EmailConfig());

        assertEquals("SMTP host and port are required.", emailService.sendEmailOrError(emailRequest()));
    }

    private static EmailRequest emailRequest() {
        final EmailRequest request = new EmailRequest();
        request.setRecipients(new String[]{"ops@example.com"});
        request.setSubject("Test Subject");
        request.setBody("<p>Test body</p>");
        return request;
    }

    private static EmailConfig validConfig() {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost("smtp.example.com");
        config.setSmtpPort(25);
        config.setFromEmail("from@example.com");
        return config;
    }
}
