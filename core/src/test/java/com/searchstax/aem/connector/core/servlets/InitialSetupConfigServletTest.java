package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialSetupConfigServletTest {

    @Mock
    private InitialSetupConfigServlet servlet;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ModifiableValueMap properties;

    private StringWriter stringWriter;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {

        servlet = new InitialSetupConfigServlet();

        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);

        Field field =
                InitialSetupConfigServlet.class.getDeclaredField("resolverUtil");

        field.setAccessible(true);
        field.set(servlet, resolverUtil);
    }

    @Test
    void testSuccess() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getParameter("./enableConnector"))
                .thenReturn("true");

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[]{"/content"});

        when(request.getParameterValues("./excludePaths"))
                .thenReturn(new String[]{"/content/exclude"});

        when(request.getParameterValues("./allowedFiles"))
                .thenReturn(new String[]{"pdf","doc"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        servlet.doPost(request,response);

        verify(properties).put("enableConnector",true);
        verify(properties).put("rootPaths",new String[]{"/content"});
        verify(properties).put("excludePaths",new String[]{"/content/exclude"});
        verify(properties).put("allowedFiles",new String[]{"pdf","doc"});

        verify(resolver).commit();
    }

    @Test
    void testRootPathMissing() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[0]);

        servlet.doPost(request,response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void testRootPathEmpty() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[]{" "});

        servlet.doPost(request,response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void testInvalidExcludePath() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[]{"/content"});

        when(request.getParameterValues("./excludePaths"))
                .thenReturn(new String[]{"/etc/test"});

        servlet.doPost(request,response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void testConfigurationNotFound() throws Exception {

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[]{"/content"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(null);

        servlet.doPost(request,response);

        verify(response).sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Configuration path not found");
    }

    @Test
    void testPropertiesNull() throws Exception {

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[]{"/content"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(null);

        servlet.doPost(request,response);

        verify(response).sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to update configuration");
    }

    @Test
    void testEmptyExcludePaths() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[]{"/content"});

        when(request.getParameterValues("./excludePaths"))
                .thenReturn(new String[0]);

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        servlet.doPost(request,response);

        verify(properties).remove("excludePaths");
    }

    @Test
    void testEmptyAllowedFiles() throws Exception {

        when(response.getWriter()).thenReturn(writer);

        doReturn(new String[]{"/content"})
                .when(request)
                .getParameterValues("./rootPaths");

        doReturn(new String[0])
                .when(request)
                .getParameterValues("./excludePaths");

        doReturn(new String[0])
                .when(request)
                .getParameterValues("./allowedFiles");

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        servlet.doPost(request, response);

        verify(properties).remove("allowedFiles");
        verify(resolver).commit();
    }

    @Test
    void testPersistenceException() throws Exception {

        when(request.getParameterValues("./rootPaths"))
                .thenReturn(new String[]{"/content"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig"))
                .thenReturn(resource);

        when(resource.adaptTo(ModifiableValueMap.class))
                .thenReturn(properties);

        doThrow(new PersistenceException("Failed"))
                .when(resolver)
                .commit();

        servlet.doPost(request,response);

        verify(response).sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to save configuration");
    }

}