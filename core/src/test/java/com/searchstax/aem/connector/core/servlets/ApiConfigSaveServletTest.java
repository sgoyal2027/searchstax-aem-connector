package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.impl.ApiConfigServiceImpl;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiConfigSaveServletTest {

    private static final String CONFIG_PATH = ApiConfigServiceImpl.CONFIG_PATH;

    @InjectMocks
    private ApiConfigSaveServlet servlet;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private CryptoSupport cryptoSupport;

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

    @Mock
    private ValueMap valueMap;

    private final Map<String, String> requestParameters = new HashMap<>();
    private StringWriter stringWriter;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        requestParameters.clear();
        when(request.getParameter(anyString())).thenAnswer(invocation ->
                requestParameters.get(invocation.getArgument(0)));
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void doPost_returnsBadRequestWhenRequiredFieldsMissing() throws Exception {
        requestParameters.put("endpointUrl", "");
        requestParameters.put("selectEndpoint", "/select");
        requestParameters.put("updateEndpoint", "/update");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Endpoint URL, Select Endpoint, and Update Endpoint are required."));
        verifyNoInteractions(resolverUtil);
    }

    @Test
    void doPost_savesConfigurationSuccessfully() throws Exception {
        stubRequiredParameters();
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(anyString(), eq(""))).thenReturn("");
        requestParameters.put("apiToken", "secret-token");
        when(cryptoSupport.protect("secret-token")).thenReturn("{protected-token}");

        servlet.doPost(request, response);

        writer.flush();
        verify(properties).put("endpointUrl", "https://api.searchstax.com");
        verify(properties).put("apiToken", "{protected-token}");
        verify(resolver).commit();
        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(stringWriter.toString().contains("\"success\":true"));
    }

    @Test
    void doPost_preservesExistingPasswordWhenPostedBlank() throws Exception {
        stubRequiredParameters();
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(anyString(), eq(""))).thenReturn("");
        when(valueMap.get("apiToken", "")).thenReturn("{existing-token}");
        requestParameters.put("apiToken", "");

        servlet.doPost(request, response);

        verify(properties).put("apiToken", "{existing-token}");
        verify(resolver).commit();
    }

    @Test
    void doPost_returnsErrorWhenConfigurationPathNotFound() throws Exception {
        stubRequiredParameters();
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(CONFIG_PATH)).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Configuration path not found");
    }

    @Test
    void doPost_returnsErrorOnPersistenceFailure() throws Exception {
        stubRequiredParameters();
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(anyString(), eq(""))).thenReturn("");
        doThrow(new PersistenceException("commit failed")).when(resolver).commit();

        servlet.doPost(request, response);

        verify(response).sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to save configuration");
    }

    private void stubRequiredParameters() {
        requestParameters.put("endpointUrl", "https://api.searchstax.com");
        requestParameters.put("selectEndpoint", "/select");
        requestParameters.put("updateEndpoint", "/update");
    }
}
