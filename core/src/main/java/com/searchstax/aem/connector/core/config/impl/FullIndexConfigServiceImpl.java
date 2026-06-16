package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.FullIndexConfigService;
import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
import com.searchstax.aem.connector.core.config.model.FullIndexIncludePathConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component(service = FullIndexConfigService.class)
public class FullIndexConfigServiceImpl
        implements FullIndexConfigService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    FullIndexConfigServiceImpl.class);

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/fullindexsetupconfig";

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public FullIndexConfig getConfiguration() {

        FullIndexConfig config =
                new FullIndexConfig();

        try (ResourceResolver resolver =
                     resolverUtil.getServiceResolver()) {

            Resource resource =
                    resolver.getResource(CONFIG_PATH);

            if (resource == null) {

                LOG.warn(
                        "Full Index configuration not found at {}",
                        CONFIG_PATH);

                return config;
            }

            ValueMap vm =
                    resource.getValueMap();

            config.setRootPath(
                    vm.get(
                            "rootPath",
                            String.class));
            Resource includePathsResource =
                    resource.getChild(
                            "includePaths");

            List<FullIndexIncludePathConfig> includePaths =
                    new ArrayList<>();

            if (includePathsResource != null) {

                for (Resource item
                        : includePathsResource.getChildren()) {

                    ValueMap itemVm =
                            item.getValueMap();

                    FullIndexIncludePathConfig includePath =
                            new FullIndexIncludePathConfig();

                    includePath.setPath(
                            itemVm.get(
                                    "path",
                                    ""));

                    includePath.setIncludeChildPath(
                            itemVm.get(
                                    "includeChildPath",
                                    false));

                    includePaths.add(
                            includePath);
                }
            }

            config.setIncludePaths(
                    includePaths);


            config.setExcludePaths(
                    vm.get(
                            "excludePaths",
                            String[].class));

            LOG.debug(
                    "Full Index configuration loaded successfully");

        } catch (LoginException e) {

            LOG.error(
                    "Unable to load Full Index configuration",
                    e);
        }

        return config;
    }
}