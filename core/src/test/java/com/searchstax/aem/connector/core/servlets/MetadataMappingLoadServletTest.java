package com.searchstax.aem.connector.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataMappingLoadServletTest {

    @InjectMocks
    private MetadataMappingLoadServlet servlet;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;

    private StringWriter stringWriter;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {

        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);

        when(response.getWriter())
                .thenReturn(writer);

        when(request.getResourceResolver())
                .thenReturn(resolver);
    }

    @Test
    void testLoadStoredMappings() throws Exception {

        String storedJson =
                "[{\"field\":\"title\"}]";

        when(resolver.getResource(anyString()))
                .thenReturn(resource);

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(valueMap.get(
                eq("metadataMappings"),
                eq("[]")))
                .thenReturn(storedJson);

        servlet.doGet(request, response);

        verify(response)
                .setContentType("application/json");

        verify(response)
                .setCharacterEncoding("UTF-8");

        assertEquals(
                storedJson,
                stringWriter.toString());
    }

    @Test
    void testResourceNotFound() throws Exception {

        when(resolver.getResource(anyString()))
                .thenReturn(null);

        servlet.doGet(request, response);

        verify(response)
                .setContentType("application/json");

        verify(response)
                .setCharacterEncoding("UTF-8");

        assertEquals(
                "[]",
                stringWriter.toString());
    }

    @Test
    void testEmptyStoredMappings() throws Exception {

        when(resolver.getResource(anyString()))
                .thenReturn(resource);

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(valueMap.get(
                eq("metadataMappings"),
                eq("[]")))
                .thenReturn("[]");

        servlet.doGet(request, response);

        assertEquals(
                "[]",
                stringWriter.toString());
    }
}