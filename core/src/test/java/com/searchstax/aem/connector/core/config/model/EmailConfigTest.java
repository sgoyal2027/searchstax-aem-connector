package com.searchstax.aem.connector.core.config.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailConfigTest {

    @Test
    void testDefaultValues() {

        EmailConfig config = new EmailConfig();

        assertNull(config.getSmtpHost());
        assertEquals(0, config.getSmtpPort());
        assertNull(config.getSmtpUser());
        assertNull(config.getSmtpPassword());
        assertNull(config.getFromEmail());
        assertNull(config.getReceiverEmails());

        assertFalse(config.isSmtpUseSsl());
        assertFalse(config.isSmtpUseStartTls());
        assertFalse(config.isSmtpRequireStartTls());
        assertFalse(config.isDebugEmail());
        assertFalse(config.isOauthFlow());
        assertFalse(config.isNotifyOnIndexingFailure());
    }

    @Test
    void testGettersAndSetters() {

        EmailConfig config = new EmailConfig();

        config.setSmtpHost("smtp.gmail.com");
        config.setSmtpPort(587);
        config.setSmtpUser("user@gmail.com");
        config.setSmtpPassword("password");
        config.setFromEmail("from@gmail.com");
        config.setReceiverEmails("user1@gmail.com,user2@gmail.com");

        config.setSmtpUseSsl(true);
        config.setSmtpUseStartTls(true);
        config.setSmtpRequireStartTls(true);
        config.setDebugEmail(true);
        config.setOauthFlow(true);
        config.setNotifyOnIndexingFailure(true);

        assertEquals("smtp.gmail.com", config.getSmtpHost());
        assertEquals(587, config.getSmtpPort());
        assertEquals("user@gmail.com", config.getSmtpUser());
        assertEquals("password", config.getSmtpPassword());
        assertEquals("from@gmail.com", config.getFromEmail());
        assertEquals("user1@gmail.com,user2@gmail.com", config.getReceiverEmails());

        assertTrue(config.isSmtpUseSsl());
        assertTrue(config.isSmtpUseStartTls());
        assertTrue(config.isSmtpRequireStartTls());
        assertTrue(config.isDebugEmail());
        assertTrue(config.isOauthFlow());
        assertTrue(config.isNotifyOnIndexingFailure());
    }
}