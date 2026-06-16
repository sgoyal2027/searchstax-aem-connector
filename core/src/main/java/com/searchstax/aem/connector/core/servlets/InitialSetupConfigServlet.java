package com.searchstax.aem.connector.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.searchstax.aem.connector.core.utils.ResolverUtil;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION
                        + "=SearchStax Initial Setup Config Servlet",
                "sling.servlet.methods=POST",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/initial-setup-config"
        }
)
public class InitialSetupConfigServlet extends SlingAllMethodsServlet {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    InitialSetupConfigServlet.class);

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/initialsetupconfig";

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    protected void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        LOG.info("Initial setup configuration request started");

        boolean enableConnector =
                Boolean.parseBoolean(
                        request.getParameter(
                                "./enableConnector"));

        String[] rootPaths =
        removeBlankValues(
                request.getParameterValues(
                        "./rootPaths"));

        String[] excludePaths =
                removeBlankValues(
                        request.getParameterValues(
                                "./excludePaths"));

        String[] allowedFiles =
                request.getParameterValues(
                        "./allowedFiles");

        /*
         * Root path validation
         */
        if (rootPaths == null || rootPaths.length == 0) {

            respondBadRequest(
                    response,
                    "At least one root path is required.");

            return;
        }

        for (String rootPath : rootPaths) {

            if (rootPath == null
                    || rootPath.trim().isEmpty()) {

                respondBadRequest(
                        response,
                        "Root path cannot be empty.");

                return;
            }
        }

        /*
         * Exclude path validation
         */
        if (excludePaths != null) {

            for (String excludePath : excludePaths) {

                if (excludePath == null
                        || excludePath.trim().isEmpty()) {
                    continue;
                }

                boolean valid = false;

                for (String rootPath : rootPaths) {

                    if (excludePath.startsWith(
                            rootPath)) {

                        valid = true;
                        break;
                    }
                }

                if (!valid) {

                    respondBadRequest(
                            response,
                            "Exclude path must be under one of the configured root paths.");

                    return;
                }
            }
        }

        /*
         * Persist configuration
         */
        try (ResourceResolver resolver =
                     resolverUtil.getServiceResolver()) {

            Resource resource =
                    resolver.getResource(
                            CONFIG_PATH);

            if (resource == null) {

                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Configuration path not found");

                return;
            }

            ModifiableValueMap properties =
                    resource.adaptTo(
                            ModifiableValueMap.class);

            if (properties == null) {

                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to update configuration");

                return;
            }

            properties.put(
                    "enableConnector",
                    enableConnector);

            properties.put(
                    "rootPaths",
                    rootPaths);

            if (excludePaths != null
                    && excludePaths.length > 0) {

                properties.put(
                        "excludePaths",
                        excludePaths);

            } else {

                properties.remove(
                        "excludePaths");
            }

            if (allowedFiles != null
                    && allowedFiles.length > 0) {

                properties.put(
                        "allowedFiles",
                        allowedFiles);

            } else {

                properties.remove(
                        "allowedFiles");
            }

            resolver.commit();

            LOG.info(
                    "Initial setup configuration saved successfully at {}",
                    CONFIG_PATH);

                response.setStatus(
                HttpServletResponse.SC_OK);

        response.setContentType(
                "application/json");

        response.setCharacterEncoding(
                "UTF-8");

        response.getWriter().write(
                "{\"success\":true,"
                        + "\"message\":\"Initial configuration saved successfully.\"}");

        } catch (PersistenceException e) {

            LOG.error(
                    "Persistence error while saving configuration",
                    e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to save configuration");

        } catch (Exception e) {

            LOG.error(
                    "Unexpected error while saving configuration",
                    e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error occurred");
        }
    }

    private void respondBadRequest(
            SlingHttpServletResponse response,
            String message)
            throws IOException {

        response.setContentType(
                "application/json");

        response.setCharacterEncoding(
                "UTF-8");

        response.setStatus(
                HttpServletResponse.SC_BAD_REQUEST);

        response.getWriter().write(
                "{\"message\":\"" + message + "\"}");
    }

    private String[] removeBlankValues(String[] values) {

    if (values == null) {
        return new String[0];
    }

    return java.util.Arrays.stream(values)
            .filter(value ->
                    value != null
                            && !value.trim().isEmpty())
            .toArray(String[]::new);
}

}