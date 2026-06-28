package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolverUtilTest {

    @InjectMocks
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Test
    void testGetServiceResolverSuccess() throws Exception {

        when(resourceResolverFactory.getServiceResourceResolver(anyMap()))
                .thenReturn(resourceResolver);

        ResourceResolver result =
                resolverUtil.getServiceResolver();

        assertSame(resourceResolver, result);

        verify(resourceResolverFactory)
                .getServiceResourceResolver(anyMap());
    }

    @Test
    void testGetServiceResolverThrowsLoginException() throws Exception {

        when(resourceResolverFactory.getServiceResourceResolver(anyMap()))
                .thenThrow(new LoginException("Login Failed"));

        assertThrows(
                LoginException.class,
                () -> resolverUtil.getServiceResolver());

        verify(resourceResolverFactory)
                .getServiceResourceResolver(anyMap());
    }
}