package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = InitialSetupConfigService.class)
public class InitialSetupConfigServiceImpl
        implements InitialSetupConfigService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    InitialSetupConfigServiceImpl.class);

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/initialsetupconfig";

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public InitialSetupConfig getConfiguration() {

        InitialSetupConfig config =
                new InitialSetupConfig();

        try (ResourceResolver resolver =
                     resolverUtil.getServiceResolver()) {

            Resource resource =
                    resolver.getResource(CONFIG_PATH);

            if (resource == null) {

                LOG.warn(
                        "Initial setup configuration not found at {}",
                        CONFIG_PATH);

                return config;
            }

            ValueMap vm = resource.getValueMap();

            config.setEnableConnector(
                    vm.get("enableConnector", false));

            config.setRootPaths(
                    vm.get("rootPaths",  new String[0]));

            config.setExcludePaths(
                    vm.get("excludePaths", new String[0]));

             config.setAllowedFiles(
                    vm.get("allowedFiles", new String[0]));

            config.setMaintenanceModeManual(vm.get("maintenanceModeManual", false));
            config.setMaintenanceMessage(vm.get("maintenanceMessage", ""));
            config.setMaintenanceFailureThreshold(vm.get("maintenanceFailureThreshold", 3));

            LOG.debug(
                    "Initial setup configuration loaded successfully");

        } catch (LoginException e) {

            LOG.error(
                    "Unable to load Initial Setup configuration",
                    e);
        }

        return config;
    }
}
