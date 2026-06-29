package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.impl.EmailConfigServiceImpl;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailConfigSaveServletTest {

    private static final String CONFIG_PATH = EmailConfigServiceImpl.CONFIG_PATH;

    @InjectMocks
    private EmailConfigSaveServlet servlet;

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
    void doPost_returnsBadRequestWhenSmtpHostMissing() throws Exception {
        requestParameters.put("smtpHost", "");
        requestParameters.put("smtpPort", "587");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("SMTP host and port are required."));
        verifyNoInteractions(resolverUtil);
    }

    @Test
    void doPost_returnsBadRequestWhenReceiverEmailsMissing() throws Exception {
        requestParameters.put("smtpHost", "smtp.example.com");
        requestParameters.put("smtpPort", "587");
        requestParameters.put("receiverEmails", "");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("At least one receiver email address is required."));
    }

    @Test
    void doPost_savesConfigurationSuccessfully() throws Exception {
        stubValidParameters();
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("smtpPassword", "")).thenReturn("");
        requestParameters.put("smtpPassword", "secret");
        when(cryptoSupport.protect("secret")).thenReturn("{protected-secret}");

        servlet.doPost(request, response);

        writer.flush();
        verify(properties).put("smtpHost", "smtp.example.com");
        verify(properties).put("smtpPassword", "{protected-secret}");
        verify(resolver).commit();
        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(stringWriter.toString().contains("Email configuration saved successfully"));
    }

    @Test
    void doPost_usesDefaultPortWhenPortInvalid() throws Exception {
        requestParameters.put("smtpHost", "smtp.example.com");
        requestParameters.put("smtpPort", "invalid");
        requestParameters.put("receiverEmails", "ops@example.com");
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("smtpPassword", "")).thenReturn("");

        servlet.doPost(request, response);

        verify(properties).put("smtpPort", 25);
    }

    @Test
    void doPost_returnsInternalErrorOnPersistenceFailure() throws Exception {
        stubValidParameters();
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("smtpPassword", "")).thenReturn("");
        doThrow(new PersistenceException("commit failed")).when(resolver).commit();

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(stringWriter.toString().contains("Unable to save configuration."));
    }

    private void stubValidParameters() {
        requestParameters.put("smtpHost", "smtp.example.com");
        requestParameters.put("smtpPort", "587");
        requestParameters.put("smtpUser", "user@example.com");
        requestParameters.put("fromEmail", "from@example.com");
        requestParameters.put("receiverEmails", "ops@example.com");
        requestParameters.put("smtpUseSSL", "true");
        requestParameters.put("smtpUseStartTLS", "off");
        requestParameters.put("notifyOnIndexingFailure", "on");
    }
}
