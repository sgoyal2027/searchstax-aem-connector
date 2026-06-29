package com.searchstax.aem.connector.core.services.impl;

import org.apache.commons.mail.EmailException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceImplErrorMessageTest {

    @Test
    void resolveEmailErrorMessage_mapsGmailAppPasswordHint() throws Exception {
        final String message = invokeResolveEmailErrorMessage(
                new EmailException("Application-specific password required"));

        assertTrue(message.contains("App Password"));
    }

    @Test
    void resolveEmailErrorMessage_mapsAuthenticationFailure() throws Exception {
        final String message = invokeResolveEmailErrorMessage(
                new EmailException("535 AuthenticationFailedException"));

        assertTrue(message.contains("SMTP authentication failed"));
    }

    @Test
    void resolveEmailErrorMessage_returnsDefaultForBlankMessage() throws Exception {
        final String message = invokeResolveEmailErrorMessage(new EmailException(""));

        assertEquals("Unable to send email.", message);
    }

    private static String invokeResolveEmailErrorMessage(final EmailException exception) throws Exception {
        final Method method =
                EmailServiceImpl.class.getDeclaredMethod("resolveEmailErrorMessage", EmailException.class);
        method.setAccessible(true);
        return (String) method.invoke(new EmailServiceImpl(), exception);
    }
}
