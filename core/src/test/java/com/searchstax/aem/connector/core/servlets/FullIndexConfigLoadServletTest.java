package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.FullIndexConfigService;
import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
import com.searchstax.aem.connector.core.config.model.FullIndexIncludePathConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FullIndexConfigLoadServletTest {

    @InjectMocks
    private FullIndexConfigLoadServlet servlet;

    @Mock
    private FullIndexConfigService configService;

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

    private FullIndexConfig buildConfig() {

        FullIndexIncludePathConfig include =
                new FullIndexIncludePathConfig();

        include.setPath("/content/site");
        include.setIncludeChildPath(true);

        FullIndexConfig config =
                new FullIndexConfig();

        config.setRootPath("/content");

        config.setIncludePaths(
                List.of(new FullIndexIncludePathConfig[]{include}));

        config.setExcludePaths(
                new String[]{"/content/site/exclude"});

        return config;
    }

    @Test
    void testLoadConfiguration() throws Exception {

        when(configService.getConfiguration())
                .thenReturn(buildConfig());

        servlet.doGet(request, response);

        verify(response)
                .setContentType("application/json");

        verify(response)
                .setCharacterEncoding("UTF-8");

        String json =
                stringWriter.toString();

        assertTrue(json.contains("/content"));
        assertTrue(json.contains("/content/site"));
        assertTrue(json.contains("/content/site/exclude"));
        assertTrue(json.contains("includeChildPath"));
    }

    @Test
    void testNullRootPath() throws Exception {

        FullIndexConfig config = buildConfig();
        config.setRootPath(null);

        when(configService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        String json = stringWriter.toString();

        assertTrue(json.contains("\"rootPath\":\"\""));
    }

    @Test
    void testNullIncludePaths() throws Exception {

        FullIndexConfig config = buildConfig();
        config.setIncludePaths(null);

        when(configService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        String json = stringWriter.toString();

        assertTrue(json.contains("\"includePaths\":[]"));
    }

    @Test
    void testEmptyExcludePaths() throws Exception {

        FullIndexConfig config = buildConfig();
        config.setExcludePaths(new String[0]);

        when(configService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        String json = stringWriter.toString();

        assertTrue(json.contains("\"excludePaths\":[]"));
    }

    @Test
    void testMultipleIncludePaths() throws Exception {

        FullIndexIncludePathConfig include1 =
                new FullIndexIncludePathConfig();
        include1.setPath("/content/site1");
        include1.setIncludeChildPath(true);

        FullIndexIncludePathConfig include2 =
                new FullIndexIncludePathConfig();
        include2.setPath("/content/site2");
        include2.setIncludeChildPath(false);

        FullIndexConfig config =
                new FullIndexConfig();

        config.setRootPath("/content");
        config.setIncludePaths(
                List.of(new FullIndexIncludePathConfig[]{
                        include1,
                        include2
                }));

        config.setExcludePaths(new String[0]);

        when(configService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        String json = stringWriter.toString();

        assertTrue(json.contains("/content/site1"));
        assertTrue(json.contains("/content/site2"));
        assertTrue(json.contains("\"includeChildPath\":true"));
        assertTrue(json.contains("\"includeChildPath\":false"));
    }

    @Test
    void testIOExceptionWhileWritingResponse() throws Exception {

        when(configService.getConfiguration())
                .thenReturn(buildConfig());

        when(response.getWriter())
                .thenThrow(new IOException("Write failed"));

        servlet.doGet(request, response);

        verify(response)
                .setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}