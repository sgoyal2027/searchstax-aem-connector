package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.impl.EmailConfigServiceImpl;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.utils.JsonServletResponseUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=SearchStax Email Config Test Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST,
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/email-config-test"
        })
public class EmailConfigTestServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConfigTestServlet.class);

    @Reference
    private transient EmailConfigService emailConfigService;

    @Reference
    private transient EmailService emailService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {

        LOG.info("Email configuration test request started");

        final EmailConfig savedConfig = emailConfigService.getConfiguration();
        final EmailConfig testConfig = buildTestConfig(request, savedConfig);
        final String[] recipients = EmailConfigServiceImpl.parseReceiverEmails(testConfig.getReceiverEmails());

        if (recipients.length == 0) {
            JsonServletResponseUtil.writeBadRequest(
                    response,
                    "Configure at least one receiver email address before sending a test email.");
            return;
        }

        final EmailRequest emailRequest = new EmailRequest();
        emailRequest.setRecipients(recipients);
        emailRequest.setSubject("SearchStax AEM Connector - test email");
        emailRequest.setBody(
                "<p>This is a test email from the SearchStax AEM Connector.</p>"
                        + "<p>If you received this message, SMTP settings are working.</p>");

        final String error = emailService.sendEmailOrError(emailRequest, testConfig);
        if (error == null) {
            JsonServletResponseUtil.writeSuccess(
                    response,
                    "Test email sent to " + recipients.length + " recipient(s).");
            return;
        }

        JsonServletResponseUtil.writeInternalError(
                response,
                error + " Check searchstaxconnector.log for additional SMTP details.");
    }

    static EmailConfig buildTestConfig(final SlingHttpServletRequest request, final EmailConfig savedConfig) {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost(firstNonBlank(request.getParameter("smtpHost"), savedConfig.getSmtpHost()));
        config.setSmtpPort(parsePort(request.getParameter("smtpPort"), savedConfig.getSmtpPort()));
        config.setSmtpUser(firstNonBlank(request.getParameter("smtpUser"), savedConfig.getSmtpUser()));
        config.setFromEmail(firstNonBlank(request.getParameter("fromEmail"), savedConfig.getFromEmail()));
        config.setReceiverEmails(firstNonBlank(request.getParameter("receiverEmails"), savedConfig.getReceiverEmails()));
        config.setSmtpUseSsl(resolveCheckbox(request, "smtpUseSSL", savedConfig.isSmtpUseSsl()));
        config.setSmtpUseStartTls(resolveCheckbox(request, "smtpUseStartTLS", savedConfig.isSmtpUseStartTls()));
        config.setSmtpRequireStartTls(config.isSmtpUseStartTls());

        final String postedPassword = trimToEmpty(request.getParameter("smtpPassword"));
        if (!postedPassword.isEmpty()) {
            config.setSmtpPassword(postedPassword);
        } else {
            config.setSmtpPassword(savedConfig.getSmtpPassword());
        }

        return config;
    }

    private static String firstNonBlank(final String primary, final String fallback) {
        final String trimmedPrimary = trimToEmpty(primary);
        if (!trimmedPrimary.isEmpty()) {
            return trimmedPrimary;
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static int parsePort(final String value, final int fallback) {
        final String trimmed = trimToEmpty(value);
        if (trimmed.isEmpty()) {
            return fallback > 0 ? fallback : 25;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return fallback > 0 ? fallback : 25;
        }
    }

    private static boolean resolveCheckbox(
            final SlingHttpServletRequest request,
            final String name,
            final boolean fallback) {
        final String value = request.getParameter(name);
        if (value == null) {
            return fallback;
        }
        return "true".equalsIgnoreCase(trimToEmpty(value)) || "on".equalsIgnoreCase(trimToEmpty(value));
    }

    private static String trimToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }
}
