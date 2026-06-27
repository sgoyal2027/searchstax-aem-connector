package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataMappingSaveServletTest {

    @InjectMocks
    private MetadataMappingSaveServlet servlet;

    @Mock
    private MetadataFieldConfigService metadataFieldConfigService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private ModifiableValueMap properties;

    private StringWriter stringWriter;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {

        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
    }

    @Test
    void testSaveMappingsSuccess() throws Exception {

        when(response.getWriter())
                .thenReturn(writer);

        when(request.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        when(request.getParameter("./metadataMappings/item0/./mappingType"))
                .thenReturn("title");

        when(request.getParameter("./metadataMappings/item0/./indexFieldName"))
                .thenReturn("title_t");

        when(request.getParameter("./metadataMappings/item0/./searchStaxFieldType"))
                .thenReturn("text");

        when(request.getParameter("./metadataMappings/item0/./enabled"))
                .thenReturn("true");

        when(request.getParameter("./metadataMappings/item0/./mandatory"))
                .thenReturn("false");

        when(request.getParameter("./metadataMappings/item1/./mappingType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(properties).put(eq("metadataMappings"), anyString());

        verify(resolver).commit();

        verify(metadataFieldConfigService).refreshMappings();

        verify(response).setContentType("application/json");
    }

    @Test
    void testSaveCustomProperty() throws Exception {

        when(response.getWriter())
                .thenReturn(writer);

        when(request.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        when(request.getParameter("./metadataMappings/item0/./mappingType"))
                .thenReturn("custom");

        when(request.getParameter("./metadataMappings/item0/./customProperty"))
                .thenReturn(" myProp ");

        when(request.getParameter("./metadataMappings/item0/./indexFieldName"))
                .thenReturn("prop_t");

        when(request.getParameter("./metadataMappings/item0/./searchStaxFieldType"))
                .thenReturn("text");

        when(request.getParameter("./metadataMappings/item0/./enabled"))
                .thenReturn("true");

        when(request.getParameter("./metadataMappings/item0/./mandatory"))
                .thenReturn("true");

        when(request.getParameter("./metadataMappings/item1/./mappingType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(properties).put(eq("metadataMappings"), contains("myProp"));

        verify(metadataFieldConfigService).refreshMappings();
    }

    @Test
    void testConfigurationPathNotFound() throws Exception {

        when(request.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(null);

        when(request.getParameter("./metadataMappings/item0/./mappingType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendError(
                HttpServletResponse.SC_NOT_FOUND,
                "Configuration path not found");

        verifyNoInteractions(metadataFieldConfigService);
    }

    @Test
    void testNoMappingsRemovesProperty() throws Exception {

        when(response.getWriter())
                .thenReturn(writer);

        when(request.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        when(request.getParameter("./metadataMappings/item0/./mappingType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(properties).remove("metadataMappings");

        verify(resolver).commit();

        verify(metadataFieldConfigService).refreshMappings();
    }
}