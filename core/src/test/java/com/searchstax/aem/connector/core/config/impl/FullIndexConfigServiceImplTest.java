package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
import com.searchstax.aem.connector.core.config.model.FullIndexIncludePathConfig;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FullIndexConfigServiceImplTest {

    @InjectMocks
    private FullIndexConfigServiceImpl service;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private Resource includePathsResource;

    @Mock
    private Resource childResource;

    @Mock
    private ValueMap valueMap;

    @Mock
    private ValueMap childValueMap;

    @Test
    void testGetConfigurationSuccess() throws Exception {

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/fullindexsetupconfig"))
                .thenReturn(resource);

        when(resource.getValueMap()).thenReturn(valueMap);

        when(valueMap.get("rootPath", String.class))
                .thenReturn("/content");

        when(valueMap.get(eq("excludePaths"), eq(String[].class)))
                .thenReturn(new String[]{"/content/exclude"});

        when(resource.getChild("includePaths"))
                .thenReturn(includePathsResource);

        when(includePathsResource.getChildren())
                .thenReturn(Collections.singletonList(childResource));

        when(childResource.getValueMap())
                .thenReturn(childValueMap);

        when(childValueMap.get("path", ""))
                .thenReturn("/content/site");

        when(childValueMap.get("includeChildPath", false))
                .thenReturn(true);

        FullIndexConfig config = service.getConfiguration();

        assertNotNull(config);
        assertEquals("/content", config.getRootPath());

        assertArrayEquals(
                new String[]{"/content/exclude"},
                config.getExcludePaths());

        List<FullIndexIncludePathConfig> includePaths =
                config.getIncludePaths();

        assertNotNull(includePaths);
        assertEquals(1, includePaths.size());

        FullIndexIncludePathConfig includePath =
                includePaths.get(0);

        assertEquals("/content/site", includePath.getPath());
        assertTrue(includePath.isIncludeChildPath());

        verify(resolver).close();
    }

    @Test
    void testGetConfigurationWithoutIncludePaths() throws Exception {

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/fullindexsetupconfig"))
                .thenReturn(resource);

        when(resource.getValueMap()).thenReturn(valueMap);

        when(resource.getChild("includePaths"))
                .thenReturn(null);

        when(valueMap.get("rootPath", String.class))
                .thenReturn("/content");

        when(valueMap.get(eq("excludePaths"), eq(String[].class)))
                .thenReturn(new String[0]);

        FullIndexConfig config = service.getConfiguration();

        assertNotNull(config);
        assertEquals("/content", config.getRootPath());
        assertTrue(config.getIncludePaths().isEmpty());

        verify(resolver).close();
    }

    @Test
    void testConfigurationNotFound() throws Exception {

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/fullindexsetupconfig"))
                .thenReturn(null);

        FullIndexConfig config = service.getConfiguration();

        assertNotNull(config);
        assertNull(config.getRootPath());
        assertNotNull(config.getIncludePaths());
        assertTrue(config.getIncludePaths().isEmpty());

        verify(resolver).close();
    }

    @Test
    void testLoginException() throws Exception {

        when(resolverUtil.getServiceResolver())
                .thenThrow(new LoginException("Login failed"));

        FullIndexConfig config = service.getConfiguration();

        assertNotNull(config);
        assertNull(config.getRootPath());
        assertNotNull(config.getIncludePaths());
        assertTrue(config.getIncludePaths().isEmpty());
    }
}