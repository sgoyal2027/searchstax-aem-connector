package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
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
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/language-mappings-load",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        }
)
public class LanguageMappingLoadServlet extends SlingSafeMethodsServlet {

    private static final Gson GSON = new Gson();

    private static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/languagemapping";

    private static final String PROPERTY_NAME = "languageMappings";

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final ResourceResolver resolver = request.getResourceResolver();
        final Resource resource = resolver.getResource(CONFIG_PATH);

        String mappingsJson = GSON.toJson(LanguageMappingConfig.defaultMappings());
        if (resource != null) {
            final String stored = resource.getValueMap().get(PROPERTY_NAME, "[]");
            if (stored != null && !stored.trim().isEmpty() && !"[]".equals(stored.trim())) {
                mappingsJson = stored;
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mappingsJson);
    }
}
