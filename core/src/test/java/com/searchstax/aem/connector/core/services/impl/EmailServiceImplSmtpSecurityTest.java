package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import org.apache.commons.mail.HtmlEmail;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceImplSmtpSecurityTest {

    @Test
    void applySmtpSecurity_sslPort465_enablesSslOnConnect() throws Exception {
        final HtmlEmail email = new HtmlEmail();
        final EmailConfig config = new EmailConfig();
        config.setSmtpPort(465);

        invokeApplySmtpSecurity(email, config);

        assertTrue(email.isSSLOnConnect());
        assertFalse(email.isStartTLSEnabled());
    }

    @Test
    void applySmtpSecurity_startTlsPort587_enablesStartTls() throws Exception {
        final HtmlEmail email = new HtmlEmail();
        final EmailConfig config = new EmailConfig();
        config.setSmtpPort(587);

        invokeApplySmtpSecurity(email, config);

        assertFalse(email.isSSLOnConnect());
        assertTrue(email.isStartTLSEnabled());
    }

    @Test
    void applySmtpSecurity_explicitFlags_overridePortDefaults() throws Exception {
        final HtmlEmail email = new HtmlEmail();
        final EmailConfig config = new EmailConfig();
        config.setSmtpPort(25);
        config.setSmtpUseSsl(true);
        config.setSmtpUseStartTls(false);

        invokeApplySmtpSecurity(email, config);

        assertTrue(email.isSSLOnConnect());
        assertFalse(email.isStartTLSEnabled());
    }

    @Test
    void applySmtpSecurity_plainSmtp_disablesSslAndStartTls() throws Exception {
        final HtmlEmail email = new HtmlEmail();
        final EmailConfig config = new EmailConfig();
        config.setSmtpPort(25);
        config.setSmtpUseSsl(false);
        config.setSmtpUseStartTls(false);

        invokeApplySmtpSecurity(email, config);

        assertFalse(email.isSSLOnConnect());
        assertFalse(email.isStartTLSEnabled());
    }

    private static void invokeApplySmtpSecurity(final HtmlEmail email, final EmailConfig config) throws Exception {
        final Method method =
                EmailServiceImpl.class.getDeclaredMethod("applySmtpSecurity", HtmlEmail.class, EmailConfig.class);
        method.setAccessible(true);
        method.invoke(new EmailServiceImpl(), email, config);
    }
}
