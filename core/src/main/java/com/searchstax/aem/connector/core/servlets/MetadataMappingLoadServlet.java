package com.searchstax.aem.connector.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/metadata-field-mappings-load",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        }
)
public class MetadataMappingLoadServlet extends SlingSafeMethodsServlet {

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/metadatafieldmapping";

    private static final String PROPERTY_NAME =
            "metadataMappings";

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws IOException {

        ResourceResolver resolver =
                request.getResourceResolver();

        Resource resource =
                resolver.getResource(CONFIG_PATH);

        String mappingsJson = "[]";

        if (resource != null) {

            mappingsJson =
                    resource.getValueMap().get(
                            PROPERTY_NAME,
                            "[]");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(mappingsJson);
    }
}