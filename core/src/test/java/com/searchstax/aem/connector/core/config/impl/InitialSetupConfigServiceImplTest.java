package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialSetupConfigServiceImplTest {

    @InjectMocks
    private InitialSetupConfigServiceImpl service;

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

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(resource);

        when(resource.getValueMap()).thenReturn(valueMap);

        when(valueMap.get("enableConnector", false))
                .thenReturn(true);

        when(valueMap.get(eq("rootPaths"), any(String[].class)))
                .thenReturn(new String[]{"/content/site1", "/content/site2"});

        when(valueMap.get(eq("excludePaths"), any(String[].class)))
                .thenReturn(new String[]{"/content/exclude"});

        when(valueMap.get(eq("allowedFiles"), any(String[].class)))
                .thenReturn(new String[]{"pdf", "docx"});

        InitialSetupConfig config = service.getConfiguration();

        assertNotNull(config);

        assertTrue(config.isEnableConnector());

        assertArrayEquals(
                new String[]{"/content/site1", "/content/site2"},
                config.getRootPaths());

        assertArrayEquals(
                new String[]{"/content/exclude"},
                config.getExcludePaths());

        assertArrayEquals(
                new String[]{"pdf", "docx"},
                config.getAllowedFiles());

        verify(resolver).close();
    }

    @Test
    void testConfigurationNotFound() throws Exception {

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(null);

        InitialSetupConfig config = service.getConfiguration();

        assertNotNull(config);

        assertFalse(config.isEnableConnector());
        assertNull(config.getRootPaths());
        assertNull(config.getExcludePaths());
        assertNull(config.getAllowedFiles());

        verify(resolver).close();
    }

    @Test
    void testLoginException() throws Exception {

        when(resolverUtil.getServiceResolver())
                .thenThrow(new LoginException("Login Failed"));

        InitialSetupConfig config = service.getConfiguration();

        assertNotNull(config);

        assertFalse(config.isEnableConnector());
        assertNull(config.getRootPaths());
        assertNull(config.getExcludePaths());
        assertNull(config.getAllowedFiles());
    }

    @Test
    void testConfigurationWithDefaultArrays() throws Exception {

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(resource);

        when(resource.getValueMap()).thenReturn(valueMap);

        when(valueMap.get("enableConnector", false))
                .thenReturn(false);

        when(valueMap.get(eq("rootPaths"), any(String[].class)))
                .thenReturn(new String[0]);

        when(valueMap.get(eq("excludePaths"), any(String[].class)))
                .thenReturn(new String[0]);

        when(valueMap.get(eq("allowedFiles"), any(String[].class)))
                .thenReturn(new String[0]);

        InitialSetupConfig config = service.getConfiguration();

        assertNotNull(config);

        assertFalse(config.isEnableConnector());

        assertEquals(0, config.getRootPaths().length);
        assertEquals(0, config.getExcludePaths().length);
        assertEquals(0, config.getAllowedFiles().length);

        verify(resolver).close();
    }
}