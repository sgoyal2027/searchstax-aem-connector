package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfigServiceImplTest {

    @Spy
    @InjectMocks
    private EmailConfigServiceImpl service;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;

    @Test
    void testGetConfigurationSuccess() throws Exception {

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);
        when(resolver.getResource(EmailConfigServiceImpl.CONFIG_PATH))
                .thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);

        when(valueMap.get("smtpHost", "")).thenReturn("smtp.gmail.com");
        when(valueMap.get("smtpPort", 25)).thenReturn(587);
        when(valueMap.get("smtpUser", "")).thenReturn("user@gmail.com");
        when(valueMap.get("smtpPassword", "")).thenReturn("password");
        when(valueMap.get("fromEmail", "")).thenReturn("from@gmail.com");
        when(valueMap.get("receiverEmails", "")).thenReturn("a@test.com,b@test.com");
        when(valueMap.get("smtpUseSSL", false)).thenReturn(true);
        when(valueMap.get("smtpUseStartTLS", false)).thenReturn(true);
        when(valueMap.get("smtpRequireStartTLS", false)).thenReturn(true);
        when(valueMap.get("debugEmail", false)).thenReturn(true);
        when(valueMap.get("oauthFlow", false)).thenReturn(true);
        when(valueMap.get("notifyOnIndexingFailure", true)).thenReturn(true);

        EmailConfig config = service.getConfiguration();

        assertEquals("smtp.gmail.com", config.getSmtpHost());
        assertEquals(587, config.getSmtpPort());
        assertEquals("user@gmail.com", config.getSmtpUser());
        assertEquals("password", config.getSmtpPassword());
        assertEquals("from@gmail.com", config.getFromEmail());
        assertEquals("a@test.com,b@test.com", config.getReceiverEmails());

        assertTrue(config.isSmtpUseSsl());
        assertTrue(config.isSmtpUseStartTls());
        assertTrue(config.isSmtpRequireStartTls());
        assertTrue(config.isDebugEmail());
        assertTrue(config.isOauthFlow());
        assertTrue(config.isNotifyOnIndexingFailure());

        verify(resolver).close();
    }

    @Test
    void testGetConfigurationResourceNotFound() throws LoginException {

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);
        when(resolver.getResource(EmailConfigServiceImpl.CONFIG_PATH))
                .thenReturn(null);

        EmailConfig config = service.getConfiguration();

        assertNotNull(config);
        assertNull(config.getSmtpHost());

        verify(resolver).close();
    }

    @Test
    void testGetConfigurationLoginException() throws Exception {

        when(resolverUtil.getServiceResolver())
                .thenThrow(new LoginException("login failed"));

        EmailConfig config = service.getConfiguration();

        assertNotNull(config);
        assertNull(config.getSmtpHost());
    }

    @Test
    void testGetReceiverAddressesEnabled() {

        EmailConfig config = new EmailConfig();
        config.setNotifyOnIndexingFailure(true);
        config.setReceiverEmails("a@test.com,b@test.com");

        doReturn(config).when(service).getConfiguration();

        String[] addresses = service.getReceiverAddresses();

        assertArrayEquals(
                new String[]{"a@test.com", "b@test.com"},
                addresses);
    }

    @Test
    void testGetReceiverAddressesDisabled() {

        EmailConfig config = new EmailConfig();
        config.setNotifyOnIndexingFailure(false);

        doReturn(config).when(service).getConfiguration();

        assertEquals(0, service.getReceiverAddresses().length);
    }

    @Test
    void testParseReceiverEmailsNull() {

        assertArrayEquals(
                new String[0],
                EmailConfigServiceImpl.parseReceiverEmails(null));
    }

    @Test
    void testParseReceiverEmailsBlank() {

        assertArrayEquals(
                new String[0],
                EmailConfigServiceImpl.parseReceiverEmails("   "));
    }

    @Test
    void testParseReceiverEmailsSingle() {

        assertArrayEquals(
                new String[]{"user@test.com"},
                EmailConfigServiceImpl.parseReceiverEmails("user@test.com"));
    }

    @Test
    void testParseReceiverEmailsMultiple() {

        assertArrayEquals(
                new String[]{
                        "a@test.com",
                        "b@test.com",
                        "c@test.com"
                },
                EmailConfigServiceImpl.parseReceiverEmails(
                        "a@test.com, b@test.com , c@test.com"));
    }

    @Test
    void testParseReceiverEmailsIgnoreEmptyValues() {

        assertArrayEquals(
                new String[]{
                        "a@test.com",
                        "b@test.com"
                },
                EmailConfigServiceImpl.parseReceiverEmails(
                        "a@test.com,, ,b@test.com,"));
    }
}