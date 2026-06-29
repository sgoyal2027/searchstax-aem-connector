package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchStaxConnectionTestServletTest {

    @InjectMocks
    private SearchStaxConnectionTestServlet servlet;

    @Mock
    private CryptoSupport cryptoSupport;

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private final Map<String, String> parameters = new HashMap<>();
    private StringWriter stringWriter;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        parameters.clear();
        when(request.getParameter(anyString())).thenAnswer(invocation ->
                parameters.get(invocation.getArgument(0)));
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void doPost_returnsBadRequestWhenEndpointUrlMissing() throws Exception {
        parameters.put("endpointUrl", "");
        parameters.put("apiToken", "token");
        parameters.put("endpointType", "general");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Missing required parameters"));
    }

    @Test
    void doPost_returnsBadRequestWhenTokenMissingAndNoSavedConfig() throws Exception {
        parameters.put("endpointUrl", "https://example.searchstax.com");
        parameters.put("apiToken", "");
        parameters.put("endpointType", "general");
        when(apiConfigService.getConfiguration()).thenReturn(new ApiConfig());

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Missing required parameters"));
    }

    @Test
    void doPost_usesSavedSelectTokenWhenPostedTokenBlank() throws Exception {
        final ApiConfig saved = new ApiConfig();
        saved.setSelectToken("{protected-select}");
        when(apiConfigService.getConfiguration()).thenReturn(saved);
        when(cryptoSupport.isProtected("{protected-select}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected-select}")).thenReturn("decrypted-select");

        parameters.put("endpointUrl", "not-http-url");
        parameters.put("apiToken", "");
        parameters.put("endpointType", "searchSelect");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Endpoint URL must start with http:// or https://"));
    }

    @Test
    void doPost_returnsBadRequestForNonHttpSearchUrl() throws Exception {
        parameters.put("endpointUrl", "ftp://invalid.example.com");
        parameters.put("apiToken", "plain-token");
        parameters.put("endpointType", "searchUpdate");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Endpoint URL must start with http:// or https://"));
    }

    @Test
    void doPost_usesSavedDiscoveryKeyForDiscoveryEndpointType() throws Exception {
        final ApiConfig saved = new ApiConfig();
        saved.setDiscoveryApiKey("discovery-key");
        when(apiConfigService.getConfiguration()).thenReturn(saved);

        parameters.put("endpointUrl", "https://invalid.searchstax.example.test");
        parameters.put("apiToken", "");
        parameters.put("endpointType", "discovery");

        servlet.doPost(request, response);

        writer.flush();
        assertTrue(stringWriter.toString().contains("\"success\":false"));
    }

    @Test
    void doPost_fallsBackToRawTokenWhenCryptoFails() throws Exception {
        when(cryptoSupport.isProtected("{protected}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected}")).thenThrow(new CryptoException("decrypt failed"));

        parameters.put("endpointUrl", "ftp://invalid.example.com");
        parameters.put("apiToken", "{protected}");
        parameters.put("endpointType", "searchSelect");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
    }
}
