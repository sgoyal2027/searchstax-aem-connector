package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/metadata-field-mappings",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST
        }
)
public class MetadataMappingSaveServlet extends SlingAllMethodsServlet {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    MetadataMappingSaveServlet.class);

    private static final String CONFIG_PATH =
                "/conf/searchstaxconnector/settings/metadatafieldmapping";

    private static final String PROPERTY_NAME =
            "metadataMappings";

    @Reference
    private MetadataFieldConfigService metadataFieldConfigService;

    private static final Gson GSON = new Gson();

    @Override
    protected void doPost(SlingHttpServletRequest request,
                          SlingHttpServletResponse response)
            throws IOException {

        List<MetadataFieldMappingConfig> mappings =
                new ArrayList<>();

        int index = 0;

        while (true) {

            String prefix =
                    "./metadataMappings/item" + index;

            String mappingType =
                    request.getParameter(
                            prefix + "/./mappingType");

            if (mappingType == null) {
                break;
            }

            MetadataFieldMappingConfig config =
                    new MetadataFieldMappingConfig();

            config.setAemField(mappingType);

        //     config.setCustomProperty(
        //             request.getParameter(
        //                     prefix + "/./customProperty"));
        String customProperty = "";

            if ("custom".equalsIgnoreCase(
                    mappingType)) {

                customProperty =
                        request.getParameter(
                                prefix + "/./customProperty");

                if (customProperty != null) {

                    customProperty =
                            customProperty.trim();
                }
            }

            config.setCustomProperty(
                    customProperty);

            config.setSearchStaxField(
                    request.getParameter(
                            prefix + "/./indexFieldName"));

            config.setSearchStaxFieldType(
                    request.getParameter(
                            prefix + "/./searchStaxFieldType"));

            config.setEnabled(
                    Boolean.parseBoolean(
                            request.getParameter(
                                    prefix + "/./enabled")));

            config.setMandatory(
                    Boolean.parseBoolean(
                            request.getParameter(
                                    prefix + "/./mandatory")));

            mappings.add(config);

            index++;
        }

        String json = GSON.toJson(mappings);

        Resource resource =
                request.getResourceResolver()
                        .getResource(CONFIG_PATH);

        if (resource == null) {

            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "Configuration path not found");

            return;
        }

        ModifiableValueMap properties =
                resource.adaptTo(ModifiableValueMap.class);

        if (mappings.isEmpty()) {
            properties.remove(PROPERTY_NAME);
            LOG.debug("No metadata mappings found. Removing property.");
        } else {
            properties.put(PROPERTY_NAME, json);
            LOG.debug("Saving {} metadata mappings", mappings.size());
        }

        request.getResourceResolver().commit();

        LOG.info(
                "Metadata field mapping configuration saved successfully. Total Mappings={}",
                mappings.size());

        metadataFieldConfigService.refreshMappings();

        response.setContentType("application/json");

        response.getWriter().write(
                "{\"success\":true}");
    }

}