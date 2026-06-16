package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.config.SiteApplicationConfigService;
import com.searchstax.aem.connector.core.config.model.SiteApplicationMappingConfig;
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
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/site-application-mappings",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST
        })
public class SiteApplicationMappingSaveServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SiteApplicationMappingSaveServlet.class);

    private static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/siteapplicationmapping";

    private static final Gson GSON = new Gson();

    @Reference
    private SiteApplicationConfigService siteApplicationConfigService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final List<SiteApplicationMappingConfig> mappings = new ArrayList<>();
        int index = 0;

        while (true) {
            final String prefix = "./siteMappings/item" + index;
            final String siteRootPath = request.getParameter(prefix + "/./siteRootPath");
            if (siteRootPath == null) {
                break;
            }

            final SiteApplicationMappingConfig config = new SiteApplicationMappingConfig();
            config.setSiteRootPath(siteRootPath);
            config.setUpdateEndpoint(request.getParameter(prefix + "/./updateEndpoint"));
            config.setUpdateToken(request.getParameter(prefix + "/./updateToken"));
            config.setSearchProfile(request.getParameter(prefix + "/./searchProfile"));
            config.setEnabled(Boolean.parseBoolean(request.getParameter(prefix + "/./enabled")));
            mappings.add(config);
            index++;
        }

        final Resource resource = request.getResourceResolver().getResource(CONFIG_PATH);
        if (resource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Configuration path not found");
            return;
        }

        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
        properties.put("siteMappings", GSON.toJson(mappings));
        request.getResourceResolver().commit();

        siteApplicationConfigService.refreshSiteMappings();
        LOG.info("Site application mapping configuration saved. Total={}", mappings.size());

        response.setContentType("application/json");
        response.getWriter().write("{\"success\":true}");
    }
}
