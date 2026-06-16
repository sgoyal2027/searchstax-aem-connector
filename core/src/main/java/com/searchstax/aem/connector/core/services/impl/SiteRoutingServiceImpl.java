package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.SiteApplicationConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.config.model.SiteApplicationMappingConfig;
import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;
import com.searchstax.aem.connector.core.services.SiteRoutingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

@Component(service = SiteRoutingService.class)
public class SiteRoutingServiceImpl implements SiteRoutingService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteRoutingServiceImpl.class);

    @Reference
    private SiteApplicationConfigService siteApplicationConfigService;

    @Reference
    private ApiConfigService apiConfigService;

    @Override
    public SiteRoutingResult resolve(final String contentPath) {
        final ApiConfig global = apiConfigService.getConfiguration();
        if (contentPath == null || contentPath.isBlank()) {
            return globalFallback(global, null);
        }

        final List<SiteApplicationMappingConfig> mappings = siteApplicationConfigService.getSiteMappings();
        final SiteApplicationMappingConfig match = mappings.stream()
                .filter(SiteApplicationMappingConfig::isEnabled)
                .filter(m -> m.getSiteRootPath() != null && !m.getSiteRootPath().isBlank())
                .filter(m -> contentPath.startsWith(normalizeRoot(m.getSiteRootPath())))
                .max(Comparator.comparingInt(m -> normalizeRoot(m.getSiteRootPath()).length()))
                .orElse(null);

        if (match == null) {
            return globalFallback(global, null);
        }

        final String endpoint = isBlank(match.getUpdateEndpoint())
                ? global.getUpdateEndpoint()
                : match.getUpdateEndpoint();
        final String token = isBlank(match.getUpdateToken())
                ? global.getUpdateToken()
                : match.getUpdateToken();

        LOG.debug(
                "Resolved site routing for path={} siteRoot={} profile={}",
                contentPath,
                match.getSiteRootPath(),
                match.getSearchProfile());

        return new SiteRoutingResult(
                endpoint,
                token,
                match.getSearchProfile(),
                match.getSiteRootPath());
    }

    private static SiteRoutingResult globalFallback(final ApiConfig global, final String siteRoot) {
        return new SiteRoutingResult(
                global.getUpdateEndpoint(),
                global.getUpdateToken(),
                null,
                siteRoot);
    }

    private static String normalizeRoot(final String root) {
        if (root == null) {
            return "";
        }
        return root.endsWith("/") ? root : root + "/";
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
