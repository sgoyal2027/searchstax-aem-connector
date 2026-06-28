package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import com.searchstax.aem.connector.core.utils.LanguageMappingConfigUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LanguageMappingLoadServletTest {

    @InjectMocks
    private LanguageMappingLoadServlet servlet;

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

    @Mock
    private ModifiableValueMap modifiableValueMap;

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
                "[{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"english\"}]";

        when(resolver.getResource(LanguageMappingConfigUtil.CONFIG_PATH))
                .thenReturn(resource);

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(valueMap.get(eq(LanguageMappingConfigUtil.PROPERTY_NAME), eq("[]")))
                .thenReturn(storedJson);

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        assertEquals(storedJson, stringWriter.toString());
    }

    @Test
    void testPersistDefaultMappingsWhenEmpty() throws Exception {

        when(resolver.getResource(LanguageMappingConfigUtil.CONFIG_PATH))
                .thenReturn(resource);

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(valueMap.get(eq(LanguageMappingConfigUtil.PROPERTY_NAME), eq("[]")))
                .thenReturn("[]");

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(modifiableValueMap);

        servlet.doGet(request, response);

        verify(modifiableValueMap)
                .put(eq(LanguageMappingConfigUtil.PROPERTY_NAME), anyString());

        verify(resolver).commit();

        String expected =
                new Gson().toJson(LanguageMappingConfig.defaultMappings());

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void testDefaultMappingsWhenResourceMissing() throws Exception {

        when(resolver.getResource(LanguageMappingConfigUtil.CONFIG_PATH))
                .thenReturn(null);

        servlet.doGet(request, response);

        String expected =
                new Gson().toJson(LanguageMappingConfig.defaultMappings());

        assertEquals(expected, stringWriter.toString());

        verify(resolver, never()).commit();
    }
}