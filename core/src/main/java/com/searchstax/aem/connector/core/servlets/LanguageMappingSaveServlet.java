package com.searchstax.aem.connector.core.servlets;

import com.google.gson.Gson;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
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
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/language-mappings",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST
        }
)
public class LanguageMappingSaveServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageMappingSaveServlet.class);

    private static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/languagemapping";

    private static final Gson GSON = new Gson();

    @Reference
    private transient LanguageConfigService languageConfigService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final List<LanguageMappingConfig> mappings = new ArrayList<>();

        int index = 0;

        while (true) {

            String prefix =
                    "./languageMappings/item" + index;

            String aemLanguageType =
                    request.getParameter(
                            prefix + "/./aemLanguageType");

            if (aemLanguageType == null) {
                break;
            }

            LanguageMappingConfig config =
                    new LanguageMappingConfig();

            config.setAemLanguage(aemLanguageType);

            config.setCustomAemLanguage(
                    request.getParameter(
                            prefix + "/./customAemLanguage"));

            config.setSearchStaxLanguage(
                    request.getParameter(
                            prefix + "/./searchStaxLanguage"));

            config.setEnabledLanguageMapping(
                    Boolean.parseBoolean(
                            request.getParameter(
                                    prefix + "/./enabledLanguageMapping")));

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

        properties.put("languageMappings", json);

        request.getResourceResolver().commit();

        LOG.info(
                "Language field mapping configuration saved successfully. Total Mappings={}",
                mappings.size());

        languageConfigService.refreshLanguageMappings();

        response.setContentType("application/json");

        response.getWriter().write(
                "{\"success\":true}");
    }
}
