package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.utils.LanguageMappingConfigUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
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

    private static final Logger LOG = LoggerFactory.getLogger(LanguageMappingLoadServlet.class);

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final ResourceResolver resolver = request.getResourceResolver();
        String mappingsJson;

        try {
            mappingsJson = LanguageMappingConfigUtil.loadOrPersistDefaultMappingsJson(resolver);
        } catch (PersistenceException e) {
            LOG.error("Failed to persist default language mappings", e);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to load language mappings");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mappingsJson);
    }
}
