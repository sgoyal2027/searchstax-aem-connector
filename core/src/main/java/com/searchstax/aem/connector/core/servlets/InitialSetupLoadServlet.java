package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
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
                        + "=SearchStax Initial Setup Load Servlet",
                "sling.servlet.methods=GET",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/initial-setup-load"
        }
)
public class InitialSetupLoadServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    InitialSetupLoadServlet.class);

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    @Reference
    private transient InitialSetupConfigService configService;

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws IOException {

        try {

            InitialSetupConfig config =
                    configService.getConfiguration();

            ObjectNode json =
                    OBJECT_MAPPER.createObjectNode();

            json.put(
                    "enableConnector",
                    config.isEnableConnector());

            ArrayNode rootPaths =
                    OBJECT_MAPPER.createArrayNode();

            for (String path
                    : config.getRootPaths()) {

                rootPaths.add(path);
            }

            json.set(
                    "rootPaths",
                    rootPaths);

            ArrayNode excludePaths =
                    OBJECT_MAPPER.createArrayNode();

            for (String path
                    : config.getExcludePaths()) {

                excludePaths.add(path);
            }

            json.set(
                    "excludePaths",
                    excludePaths);

            ArrayNode allowedFiles =
                    OBJECT_MAPPER.createArrayNode();

            for (String fileType
                    : config.getAllowedFiles()) {

                allowedFiles.add(fileType);
            }

            json.set(
                    "allowedFiles",
                    allowedFiles);

            json.put("maintenanceModeManual", config.isMaintenanceModeManual());
            json.put("maintenanceMessage", config.getMaintenanceMessage() != null ? config.getMaintenanceMessage() : "");
            json.put("maintenanceFailureThreshold", config.getMaintenanceFailureThreshold());

            response.setContentType(
                    "application/json");

            response.setCharacterEncoding(
                    "UTF-8");

            response.getWriter()
                    .print(json.toString());

        } catch (Exception e) {

            LOG.error(
                    "Error loading Initial Setup configuration",
                    e);

            response.setStatus(
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            ObjectNode errorJson =
                    OBJECT_MAPPER.createObjectNode();

            errorJson.put(
                    "error",
                    "Unable to load configuration");

            response.getWriter()
                    .print(errorJson.toString());
        }
    }
}