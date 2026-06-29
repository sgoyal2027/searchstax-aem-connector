package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class FullIndexConfigSaveServletTest {

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/fullindexsetupconfig";

    private final AemContext context = AppAemContext.newAemContext();

    @InjectMocks
    private FullIndexConfigSaveServlet servlet;

    @Mock
    private ResolverUtil resolverUtil;

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
    }

    private void stubWriter() throws Exception {
        when(response.getWriter()).thenReturn(writer);
    }

    private void stubResolver() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void doPost_returnsBadRequestWhenRootPathMissing() throws Exception {
        stubWriter();
        when(request.getParameter("./rootPath")).thenReturn("");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Root path is required."));
    }

    @Test
    void doPost_returnsBadRequestWhenIncludePathOutsideRoot() throws Exception {
        stubWriter();
        when(request.getParameter("./rootPath")).thenReturn("/content/site");
        when(request.getParameter("./includePathsJson")).thenReturn(
                "[{\"path\":\"/content/other\",\"includeChildPath\":true}]");

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Include paths must be under root path."));
    }

    @Test
    void doPost_returnsBadRequestWhenExcludePathOutsideRoot() throws Exception {
        stubWriter();
        when(request.getParameter("./rootPath")).thenReturn("/content/site");
        when(request.getParameter("./includePathsJson")).thenReturn("[]");
        when(request.getParameterValues("./excludePaths"))
                .thenReturn(new String[]{"/content/other/exclude"});

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(stringWriter.toString().contains("Exclude paths must be under root path."));
    }

    @Test
    void doPost_returnsErrorWhenConfigurationPathNotFound() throws Exception {
        stubWriter();
        stubResolver();
        when(request.getParameter("./rootPath")).thenReturn("/content/site");
        when(request.getParameter("./includePathsJson")).thenReturn("[]");
        when(request.getParameterValues("./excludePaths")).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Configuration path not found");
    }

    @Test
    void doPost_savesConfigurationSuccessfully() throws Exception {
        stubWriter();
        stubResolver();
        context.create().resource(CONFIG_PATH, Map.of("jcr:primaryType", "nt:unstructured"));

        when(request.getParameter("./rootPath")).thenReturn("/content/site");
        when(request.getParameter("./includePathsJson")).thenReturn(
                "[{\"path\":\"/content/site/en\",\"includeChildPath\":true}]");
        when(request.getParameterValues("./excludePaths"))
                .thenReturn(new String[]{"/content/site/en/private", ""});

        servlet.doPost(request, response);

        writer.flush();
        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(stringWriter.toString().contains("\"success\":true"));

        final Resource saved = context.resourceResolver().getResource(CONFIG_PATH);
        assertEquals("/content/site", saved.getValueMap().get("rootPath", String.class));
        assertTrue(saved.getChild("includePaths") != null);
    }
}
