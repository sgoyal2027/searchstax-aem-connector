package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.model.FullIndexIncludePathConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION
                        + "=SearchStax Full Index Config Servlet",
                "sling.servlet.methods=POST",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/indexing-config-save"
        }
)
public class FullIndexConfigSaveServlet
        extends SlingAllMethodsServlet {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    FullIndexConfigSaveServlet.class);

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/indexingconfig";

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    protected void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        LOG.info(
                "Indexing configuration save started");

        boolean enableConnector =
                Boolean.parseBoolean(
                        request.getParameter(
                                "./enableConnector"));

        String[] rootPaths =
        removeBlankValues(
                request.getParameterValues(
                        "./rootPaths"));

        String includePathsJson =
        request.getParameter(
                "./includePathsJson");

        List<FullIndexIncludePathConfig> includePaths;

        if(includePathsJson !=null
            && !includePathsJson.trim().isEmpty()) {

            includePaths = OBJECT_MAPPER.readValue(
                    includePathsJson,
                    new TypeReference<
                            List<FullIndexIncludePathConfig>>() {
                    });
        } else {
            includePaths = List.of();
        }

        String[] excludePaths =
                removeBlankValues(
                        request.getParameterValues(
                                "./excludePaths"));

        String[] allowedFiles =
                request.getParameterValues(
                        "./allowedFiles");

        LOG.info("Root Paths: {}", Arrays.toString(rootPaths));
        LOG.info("IncludePathsJson: {}", includePathsJson);
        LOG.info("Exclude Paths: {}", Arrays.toString(excludePaths));

        /*
         * Validation
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
         * Include Path Validation
         */
        for (FullIndexIncludePathConfig includePath : includePaths) {

                boolean valid = false;

                for (String rootPath : rootPaths) {
                        if (includePath.getPath().startsWith(rootPath)) {
                        valid = true;
                        break;
                        }
                }

                if (!valid) {
                        respondBadRequest(
                                response,
                                "Include paths must be under root paths.");
                        return;
                }
        }

        /*
         * Exclude path validation
         */
        for (String excludePath : excludePaths) {

                if (excludePath == null
                        || excludePath.trim().isEmpty()) {
                        continue;
                }

                boolean valid = false;

                for (String rootPath : rootPaths) {

                        if (excludePath.startsWith(rootPath)) {
                        valid = true;
                        break;
                        }
                }

                if (!valid) {
                        respondBadRequest(
                                response,
                                "Exclude paths must be under root paths.");
                        return;
                }
                }

        try (ResourceResolver resolver =
                     resolverUtil.getServiceResolver()) {

            Resource resource =
                    resolver.getResource(
                            CONFIG_PATH);

            if (resource == null) {

                LOG.error(
                        "Configuration resource not found at {}",
                        CONFIG_PATH);

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

            /*
             * Root Path
             */
            properties.put(
                    "rootPaths",
                    rootPaths);

            /*
             * Include Paths (Composite Multifield)
             */
            Resource includePathsResource =
                    resource.getChild(
                            "includePaths");

            if (includePathsResource != null) {

                resolver.delete(
                        includePathsResource);
            }

            includePathsResource =
                    ResourceUtil.getOrCreateResource(
                            resolver,
                            CONFIG_PATH + "/includePaths",
                            "nt:unstructured",
                            "nt:unstructured",
                            false);

            for (int i = 0;
                 i < includePaths.size();
                 i++) {

                FullIndexIncludePathConfig includePath =
                        includePaths.get(i);

                Map<String, Object> itemProperties =
                        new HashMap<>();

                itemProperties.put(
                        "path",
                        includePath.getPath());

                itemProperties.put(
                        "includeChildPath",
                        includePath.isIncludeChildPath());

                ResourceUtil.getOrCreateResource(
                        resolver,
                        includePathsResource.getPath()
                                + "/item" + i,
                        itemProperties,
                        "nt:unstructured",
                        false);
            }


            /*
             * Exclude Paths
             */
            if (excludePaths.length > 0) {

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
                    "Indexing configuration saved successfully");

            response.setStatus(
                    HttpServletResponse.SC_OK);

            response.setContentType(
                    "application/json");

            response.setCharacterEncoding(
                    "UTF-8");

            response.getWriter().write(
                    "{\"success\":true,"
                            + "\"message\":\"Indexing configuration saved successfully.\"}");

        } catch (PersistenceException e) {

            LOG.error(
                    "Persistence error while saving Indexing configuration",
                    e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to save configuration");

        } catch (Exception e) {

            LOG.error(
                    "Unexpected error while saving Indexing configuration",
                    e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error occurred");
            
        }
    }

    private String[] removeBlankValues(
            String[] values) {

        if (values == null) {

            return new String[0];
        }

        return Arrays.stream(values)
                .filter(value ->
                        value != null
                                && !value.trim().isEmpty())
                .toArray(String[]::new);
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
                "{\"message\":\""
                        + message
                        + "\"}");
    }
}