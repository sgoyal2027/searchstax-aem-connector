package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.LanguageConfigService;
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
class LanguageMappingSaveServletTest {

    @InjectMocks
    private LanguageMappingSaveServlet servlet;

    @Mock
    private LanguageConfigService languageConfigService;

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
    void testSaveLanguageMappings() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getResourceResolver()).thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/languagemapping"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        when(request.getParameter("./languageMappings/item0/./aemLanguageType"))
                .thenReturn("en");

        when(request.getParameter("./languageMappings/item0/./customAemLanguage"))
                .thenReturn("");

        when(request.getParameter("./languageMappings/item0/./searchStaxLanguage"))
                .thenReturn("english");

        when(request.getParameter("./languageMappings/item0/./enabledLanguageMapping"))
                .thenReturn("true");

        when(request.getParameter("./languageMappings/item1/./aemLanguageType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(properties)
                .put(eq("languageMappings"), anyString());

        verify(resolver).commit();

        verify(languageConfigService)
                .refreshLanguageMappings();

        verify(response)
                .setContentType("application/json");
    }

    @Test
    void testConfigurationPathNotFound() throws Exception {

        when(request.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/languagemapping"))
                .thenReturn(null);

        when(request.getParameter("./languageMappings/item0/./aemLanguageType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendError(
                HttpServletResponse.SC_NOT_FOUND,
                "Configuration path not found");

        verifyNoInteractions(languageConfigService);
    }

    @Test
    void testCustomLanguageMapping() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/languagemapping"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        when(request.getParameter("./languageMappings/item0/./aemLanguageType"))
                .thenReturn("custom");

        when(request.getParameter("./languageMappings/item0/./customAemLanguage"))
                .thenReturn("gu_IN");

        when(request.getParameter("./languageMappings/item0/./searchStaxLanguage"))
                .thenReturn("gujarati");

        when(request.getParameter("./languageMappings/item0/./enabledLanguageMapping"))
                .thenReturn("true");

        when(request.getParameter("./languageMappings/item1/./aemLanguageType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(properties)
                .put(eq("languageMappings"), contains("gu_IN"));

        verify(languageConfigService)
                .refreshLanguageMappings();
    }

    @Test
    void testEmptyMappings() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/languagemapping"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        when(request.getParameter("./languageMappings/item0/./aemLanguageType"))
                .thenReturn(null);

        servlet.doPost(request, response);

        verify(properties)
                .put(eq("languageMappings"), eq("[]"));

        verify(resolver).commit();

        verify(languageConfigService)
                .refreshLanguageMappings();
    }
}