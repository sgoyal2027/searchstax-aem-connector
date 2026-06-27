package com.searchstax.aem.connector.core.servlets;

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

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiConfigLoadServletTest {

    @InjectMocks
    private ApiConfigLoadServlet servlet;

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {

        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);

        when(response.getWriter())
                .thenReturn(writer);
    }

    @Test
    void testDoGetSuccess() throws Exception {

        ApiConfig config = new ApiConfig();

        config.setEndpointUrl("https://api.searchstax.com");
        config.setSelectEndpoint("/select");
        config.setUpdateEndpoint("/update");
        config.setAutoSuggestApi("/suggest");
        config.setRelatedSearchesEndpoint("/related");
        config.setPopularSearchesEndpoint("/popular");
        config.setAnalyticsTrackingUrl("/tracking");
        config.setAnalyticsReportingUrl("/reporting");
        config.setForwardGeocodingEndpoint("/forward");
        config.setReverseGeocodingEndpoint("/reverse");

        config.setApiToken("token");
        config.setSelectToken("select");
        config.setUpdateToken("update");
        config.setDiscoveryApiKey("discovery");
        config.setAnalyticsTrackingKey("tracking");
        config.setAnalyticsReportingApiKey("reporting");

        when(apiConfigService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        writer.flush();

        String json = stringWriter.toString();

        verify(response)
                .setContentType("application/json");

        verify(response)
                .setCharacterEncoding("UTF-8");

        assertTrue(json.contains("\"endpointUrl\":\"https://api.searchstax.com\""));
        assertTrue(json.contains("\"selectEndpoint\":\"/select\""));
        assertTrue(json.contains("\"updateEndpoint\":\"/update\""));

        assertTrue(json.contains("\"apiToken\":\"\""));
        assertTrue(json.contains("\"selectToken\":\"\""));
        assertTrue(json.contains("\"updateToken\":\"\""));

        assertTrue(json.contains("\"apiTokenConfigured\":true"));
        assertTrue(json.contains("\"selectTokenConfigured\":true"));
        assertTrue(json.contains("\"updateTokenConfigured\":true"));
    }

    @Test
    void testDoGetException() throws Exception {

        when(apiConfigService.getConfiguration())
                .thenThrow(new RuntimeException("Failure"));

        servlet.doGet(request, response);

        writer.flush();

        verify(response)
                .setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        assertTrue(
                stringWriter.toString()
                        .contains("Unable to load configuration"));
    }
}