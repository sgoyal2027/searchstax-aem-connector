package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigResourceUtilTest {

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private Resource parent;

    @Mock
    private ModifiableValueMap valueMap;

    @Test
    void testGetOrCreateConfigResourceAlreadyExists() throws Exception {

        when(resolver.getResource("/conf/test"))
                .thenReturn(resource);

        Resource result =
                ConfigResourceUtil.getOrCreateConfigResource(
                        resolver,
                        "/conf/test");

        assertSame(resource, result);

        verify(resolver, never())
                .create(any(), anyString(), anyMap());
    }

    @Test
    void testGetOrCreateConfigResourceCreated() throws Exception {

        when(resolver.getResource("/conf/test"))
                .thenReturn(null);

        when(resolver.getResource("/conf"))
                .thenReturn(parent);

        when(resolver.create(
                eq(parent),
                eq("test"),
                anyMap()))
                .thenReturn(resource);

        Resource result =
                ConfigResourceUtil.getOrCreateConfigResource(
                        resolver,
                        "/conf/test");

        assertSame(resource, result);

        verify(resolver)
                .create(
                        eq(parent),
                        eq("test"),
                        anyMap());
    }

    @Test
    void testGetOrCreateConfigResourceParentNotFound() throws Exception {

        when(resolver.getResource("/conf/test"))
                .thenReturn(null);

        when(resolver.getResource("/conf"))
                .thenReturn(null);

        Resource result =
                ConfigResourceUtil.getOrCreateConfigResource(
                        resolver,
                        "/conf/test");

        assertNull(result);

        verify(resolver, never())
                .create(any(), anyString(), anyMap());
    }

    @Test
    void testGetOrCreateConfigResourceInvalidPath() throws Exception {

        Resource result =
                ConfigResourceUtil.getOrCreateConfigResource(
                        resolver,
                        "invalid");

        assertNull(result);

        verify(resolver, never())
                .create(any(), anyString(), anyMap());
    }

    @Test
    void testGetModifiableProperties() {

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(valueMap);

        ModifiableValueMap result =
                ConfigResourceUtil.getModifiableProperties(resource);

        assertSame(valueMap, result);
    }

    @Test
    void testGetModifiablePropertiesNull() {

        assertNull(
                ConfigResourceUtil.getModifiableProperties(null));
    }
}