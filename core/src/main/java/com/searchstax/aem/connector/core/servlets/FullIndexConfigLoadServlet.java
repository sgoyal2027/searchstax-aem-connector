package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.config.FullIndexConfigService;
import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
import com.searchstax.aem.connector.core.config.model.FullIndexIncludePathConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION
                        + "=SearchStax Full Index Configuration Load Servlet",
                "sling.servlet.methods=GET",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/full-index-load"
        }
)
public class FullIndexConfigLoadServlet
        extends SlingSafeMethodsServlet {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    FullIndexConfigLoadServlet.class);

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    @Reference
    private transient FullIndexConfigService configService;

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws IOException {

        try {

            FullIndexConfig config =
                    configService.getConfiguration();

            ObjectNode json =
                    OBJECT_MAPPER.createObjectNode();

            /*
             * Root Path
             */
            json.put(
                    "rootPath",
                    config.getRootPath() != null
                            ? config.getRootPath()
                            : "");
            /*
             * Include Paths
             */
            ArrayNode includePaths =
                    OBJECT_MAPPER.createArrayNode();

            if (config.getIncludePaths() != null) {

                for (FullIndexIncludePathConfig includePath
                        : config.getIncludePaths()) {

                    ObjectNode includePathJson =
                            OBJECT_MAPPER.createObjectNode();

                    includePathJson.put(
                            "path",
                            includePath.getPath());

                    includePathJson.put(
                            "includeChildPath",
                            includePath.isIncludeChildPath());

                    includePaths.add(
                            includePathJson);
                }
            }

            json.set(
                    "includePaths",
                    includePaths);

            /*
             * Exclude Paths
             */
            ArrayNode excludePaths =
                    OBJECT_MAPPER.createArrayNode();

            for (String path
                    : config.getExcludePaths()) {

                excludePaths.add(path);
            }

            json.set(
                    "excludePaths",
                    excludePaths);

            response.setContentType(
                    "application/json");

            response.setCharacterEncoding(
                    "UTF-8");

            response.getWriter()
                    .print(
                            json.toString());

        } catch (IOException e) {

            LOG.error(
                    "Error writing Full Index configuration response",
                    e);

            response.setStatus(
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            ObjectNode errorJson =
                    OBJECT_MAPPER.createObjectNode();

            errorJson.put(
                    "error",
                    "Unable to load configuration");

            response.getWriter()
                    .print(
                            errorJson.toString());
        }
    }
}