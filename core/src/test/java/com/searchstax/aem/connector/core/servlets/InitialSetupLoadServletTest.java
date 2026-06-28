package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialSetupLoadServletTest {

    @InjectMocks
    private InitialSetupLoadServlet servlet;

    @Mock
    private InitialSetupConfigService configService;

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

    private InitialSetupConfig buildConfig() {

        InitialSetupConfig config =
                new InitialSetupConfig();

        config.setEnableConnector(true);

        config.setRootPaths(new String[]{
                "/content/site1",
                "/content/site2"
        });

        config.setExcludePaths(new String[]{
                "/content/site1/exclude"
        });

        config.setAllowedFiles(new String[]{
                "pdf",
                "docx"
        });

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

        assertTrue(json.contains("\"enableConnector\":true"));
        assertTrue(json.contains("/content/site1"));
        assertTrue(json.contains("/content/site2"));
        assertTrue(json.contains("/content/site1/exclude"));
        assertTrue(json.contains("pdf"));
        assertTrue(json.contains("docx"));
    }

    @Test
    void testEmptyArrays() throws Exception {

        InitialSetupConfig config =
                new InitialSetupConfig();

        config.setEnableConnector(false);
        config.setRootPaths(new String[0]);
        config.setExcludePaths(new String[0]);
        config.setAllowedFiles(new String[0]);

        when(configService.getConfiguration())
                .thenReturn(config);

        servlet.doGet(request, response);

        String json =
                stringWriter.toString();

        assertTrue(json.contains("\"rootPaths\":[]"));
        assertTrue(json.contains("\"excludePaths\":[]"));
        assertTrue(json.contains("\"allowedFiles\":[]"));
    }
}