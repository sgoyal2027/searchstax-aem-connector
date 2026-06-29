package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.SiteApplicationConfigService;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiteApplicationMappingSaveServletTest {

    private static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/siteapplicationmapping";

    @InjectMocks
    private SiteApplicationMappingSaveServlet servlet;

    @Mock
    private SiteApplicationConfigService siteApplicationConfigService;

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
        when(response.getWriter()).thenReturn(writer);
        when(request.getResourceResolver()).thenReturn(resolver);
    }

    @Test
    void doPost_savesMappingsSuccessfully() throws Exception {
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(request.getParameter("./siteMappings/item0/./siteRootPath")).thenReturn("/content/wknd");
        when(request.getParameter("./siteMappings/item0/./updateEndpoint")).thenReturn("/update");
        when(request.getParameter("./siteMappings/item0/./updateToken")).thenReturn("token");
        when(request.getParameter("./siteMappings/item0/./searchProfile")).thenReturn("default");
        when(request.getParameter("./siteMappings/item0/./enabled")).thenReturn("true");
        when(request.getParameter("./siteMappings/item1/./siteRootPath")).thenReturn(null);

        servlet.doPost(request, response);

        writer.flush();
        verify(properties).put(eq("siteMappings"), anyString());
        verify(resolver).commit();
        verify(siteApplicationConfigService).refreshSiteMappings();
        verify(response).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("\"success\":true"));
    }

    @Test
    void doPost_returnsNotFoundWhenConfigPathMissing() throws Exception {
        when(resolver.getResource(CONFIG_PATH)).thenReturn(null);
        when(request.getParameter("./siteMappings/item0/./siteRootPath")).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Configuration path not found");
        verifyNoInteractions(siteApplicationConfigService);
    }

    @Test
    void doPost_savesEmptyMappingsWhenNoItemsProvided() throws Exception {
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(request.getParameter("./siteMappings/item0/./siteRootPath")).thenReturn(null);

        servlet.doPost(request, response);

        verify(properties).put(eq("siteMappings"), eq("[]"));
        verify(siteApplicationConfigService).refreshSiteMappings();
    }
}
