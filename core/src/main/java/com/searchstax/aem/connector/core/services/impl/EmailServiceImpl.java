package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = EmailService.class)
public class EmailServiceImpl implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailServiceImpl.class);

    private static final int GMAIL_SSL_PORT = 465;
    private static final int GMAIL_STARTTLS_PORT = 587;

    @Reference
    private EmailConfigService emailConfigService;

    @Reference
    private ProtectedValueCodec protectedValueCodec;

    @Override
    public boolean sendEmail(final EmailRequest request) {
        return sendEmailOrError(request) == null;
    }

    @Override
    public String sendEmailOrError(final EmailRequest request) {
        return sendEmailOrError(request, emailConfigService.getConfiguration());
    }

    @Override
    public String sendEmailOrError(final EmailRequest request, final EmailConfig config) {
        if (request == null || request.getRecipients() == null || request.getRecipients().length == 0) {
            LOG.warn("Email request skipped: no recipients");
            return "At least one receiver email address is required.";
        }

        if (!isSmtpConfigured(config)) {
            LOG.error("Email request skipped: SMTP is not configured in Author UI");
            return "SMTP host and port are required.";
        }

        try {
            final String smtpPassword = resolveSmtpPassword(config);
            if (hasSmtpCredentials(config) && smtpPassword.isEmpty()) {
                LOG.error("Email request skipped: SMTP password is missing or could not be decrypted");
                return "SMTP password is missing or could not be decrypted. Enter the password and save, "
                        + "or re-enter it before sending a test email.";
            }

            final HtmlEmail email = new HtmlEmail();
            email.setHostName(config.getSmtpHost().trim());
            email.setSmtpPort(config.getSmtpPort());
            applySmtpSecurity(email, config);

            if (hasSmtpCredentials(config)) {
                email.setAuthenticator(new DefaultAuthenticator(config.getSmtpUser().trim(), smtpPassword));
            }

            if (config.getFromEmail() != null && !config.getFromEmail().trim().isEmpty()) {
                email.setFrom(config.getFromEmail().trim());
            } else if (hasSmtpCredentials(config)) {
                email.setFrom(config.getSmtpUser().trim());
            }

            email.setSubject(request.getSubject());
            email.setHtmlMsg(request.getBody());

            for (final String recipient : request.getRecipients()) {
                if (recipient != null && !recipient.trim().isEmpty()) {
                    email.addTo(recipient.trim());
                }
            }

            email.send();
            LOG.info("Email notification sent successfully to {} recipient(s)", request.getRecipients().length);
            return null;
        } catch (EmailException e) {
            final String message = resolveEmailErrorMessage(e);
            LOG.error("Failed to send email notification: {}", message, e);
            return message;
        }
    }

    private String resolveSmtpPassword(final EmailConfig config) {
        final String decrypted = protectedValueCodec.unprotectIfNeeded(config.getSmtpPassword());
        if (protectedValueCodec.looksEncrypted(decrypted)) {
            LOG.error("SMTP password is still encrypted after decryption. Re-save Email Configuration.");
            return "";
        }
        return decrypted;
    }

    private void applySmtpSecurity(final HtmlEmail email, final EmailConfig config) throws EmailException {
        final int port = config.getSmtpPort();
        final boolean useSsl = config.isSmtpUseSsl() || port == GMAIL_SSL_PORT;
        final boolean useStartTls = config.isSmtpUseStartTls() || port == GMAIL_STARTTLS_PORT;

        if (useSsl && !useStartTls) {
            email.setSSLOnConnect(true);
            email.setSslSmtpPort(String.valueOf(port > 0 ? port : GMAIL_SSL_PORT));
            email.setStartTLSEnabled(false);
            email.setStartTLSRequired(false);
            return;
        }

        if (useStartTls) {
            email.setSSLOnConnect(false);
            email.setStartTLSEnabled(true);
            email.setStartTLSRequired(config.isSmtpRequireStartTls());
            return;
        }

        email.setSSLOnConnect(false);
        email.setStartTLSEnabled(false);
        email.setStartTLSRequired(false);
    }

    private boolean hasSmtpCredentials(final EmailConfig config) {
        return config.getSmtpUser() != null && !config.getSmtpUser().trim().isEmpty();
    }

    private boolean isSmtpConfigured(final EmailConfig config) {
        return config.getSmtpHost() != null
                && !config.getSmtpHost().trim().isEmpty()
                && config.getSmtpPort() > 0;
    }

    private String resolveEmailErrorMessage(final EmailException exception) {
        final String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("Application-specific password required")) {
            return "Gmail rejected the SMTP login. Create a Google App Password and use it as the SMTP password "
                    + "(https://support.google.com/accounts/answer/185833).";
        }
        if (message.contains("AuthenticationFailedException") || message.contains("535")) {
            return "SMTP authentication failed. Verify SMTP user/password and SSL/TLS settings.";
        }
        return message.isEmpty() ? "Unable to send email." : message;
    }
}
