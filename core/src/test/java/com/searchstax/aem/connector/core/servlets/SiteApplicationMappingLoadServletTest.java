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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteApplicationMappingLoadServletTest {

    private static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/siteapplicationmapping";

    @InjectMocks
    private SiteApplicationMappingLoadServlet servlet;

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

    @BeforeEach
    void setup() throws Exception {
        stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(request.getResourceResolver()).thenReturn(resolver);
    }

    @Test
    void doGet_returnsStoredMappingsJson() throws Exception {
        final String storedJson = "[{\"siteRootPath\":\"/content/wknd\"}]";
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(eq("siteMappings"), eq("[]"))).thenReturn(storedJson);

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertEquals(storedJson, stringWriter.toString());
    }

    @Test
    void doGet_returnsEmptyArrayWhenConfigMissing() throws Exception {
        when(resolver.getResource(CONFIG_PATH)).thenReturn(null);

        servlet.doGet(request, response);

        assertEquals("[]", stringWriter.toString());
    }
}
