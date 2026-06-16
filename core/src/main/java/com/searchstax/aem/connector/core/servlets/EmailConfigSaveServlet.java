package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.impl.EmailConfigServiceImpl;
import com.searchstax.aem.connector.core.utils.ConfigResourceUtil;
import com.searchstax.aem.connector.core.utils.JsonServletResponseUtil;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=SearchStax Email Config Save Servlet",
                "sling.servlet.methods=POST",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/email-config-save"
        })
public class EmailConfigSaveServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConfigSaveServlet.class);

    @Reference
    private transient ResolverUtil resolverUtil;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        LOG.info("Email configuration save request started");

        if (!validateRequiredFields(request, response)) {
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource resource = ConfigResourceUtil.getOrCreateConfigResource(
                    resolver,
                    EmailConfigServiceImpl.CONFIG_PATH);
            if (resource == null) {
                JsonServletResponseUtil.writeInternalError(response, "Configuration path not found.");
                return;
            }

            final ModifiableValueMap properties = ConfigResourceUtil.getModifiableProperties(resource);
            if (properties == null) {
                JsonServletResponseUtil.writeInternalError(response, "Unable to update configuration.");
                return;
            }

            final ValueMap existing = resource.getValueMap();

            properties.put("smtpHost", trimToEmpty(request.getParameter("smtpHost")));
            properties.put("smtpPort", parsePort(request.getParameter("smtpPort")));
            properties.put("smtpUser", trimToEmpty(request.getParameter("smtpUser")));
            putPasswordProperty(properties, existing, request.getParameter("smtpPassword"));
            properties.put("fromEmail", trimToEmpty(request.getParameter("fromEmail")));
            properties.put("receiverEmails", trimToEmpty(request.getParameter("receiverEmails")));
            properties.put("smtpUseSSL", isChecked(request, "smtpUseSSL"));
            properties.put("smtpUseStartTLS", isChecked(request, "smtpUseStartTLS"));
            properties.put("smtpRequireStartTLS", isChecked(request, "smtpUseStartTLS"));
            properties.put("debugEmail", false);
            properties.put("oauthFlow", false);
            properties.put("notifyOnIndexingFailure", isChecked(request, "notifyOnIndexingFailure"));

            resolver.commit();
            LOG.info("Email configuration saved at {}", EmailConfigServiceImpl.CONFIG_PATH);
            JsonServletResponseUtil.writeSuccess(response, "Email configuration saved successfully.");
        } catch (PersistenceException e) {
            LOG.error("Persistence error while saving email configuration", e);
            JsonServletResponseUtil.writeInternalError(response, "Unable to save configuration.");
        } catch (Exception e) {
            LOG.error("Unexpected error while saving email configuration", e);
            JsonServletResponseUtil.writeInternalError(response, "Unexpected error occurred.");
        }
    }

    private boolean validateRequiredFields(
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws IOException {
        if (isBlank(request.getParameter("smtpHost"))
                || isBlank(request.getParameter("smtpPort"))) {
            respondBadRequest(response, "SMTP host and port are required.");
            return false;
        }
        if (isBlank(request.getParameter("receiverEmails"))) {
            respondBadRequest(response, "At least one receiver email address is required.");
            return false;
        }
        return true;
    }

    private void putPasswordProperty(
            final ModifiableValueMap properties,
            final ValueMap existing,
            final String posted) {
        final String trimmed = trimToEmpty(posted);
        if (!trimmed.isEmpty()) {
            properties.put("smtpPassword", protect(trimmed));
            return;
        }
        final String existingPassword = existing.get("smtpPassword", "");
        if (!existingPassword.isEmpty()) {
            properties.put("smtpPassword", existingPassword);
        } else {
            properties.remove("smtpPassword");
        }
    }

    private String protect(final String plaintext) {
        if (cryptoSupport == null) {
            return plaintext;
        }
        try {
            return cryptoSupport.protect(plaintext);
        } catch (CryptoException e) {
            LOG.warn("Failed to encrypt SMTP password; storing plaintext", e);
            return plaintext;
        }
    }

    private static int parsePort(final String value) {
        try {
            return Integer.parseInt(trimToEmpty(value));
        } catch (NumberFormatException e) {
            return 25;
        }
    }

    private static boolean isChecked(final SlingHttpServletRequest request, final String name) {
        final String value = request.getParameter(name);
        return "true".equalsIgnoreCase(trimToEmpty(value)) || "on".equalsIgnoreCase(trimToEmpty(value));
    }

    private static void respondBadRequest(final SlingHttpServletResponse response, final String message)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    private static String trimToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(final String value) {
        return trimToEmpty(value).isEmpty();
    }
}
