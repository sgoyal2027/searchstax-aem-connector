package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;

/**
 * Resolves SearchStax update endpoint, token, and search profile for a content path.
 */
public interface SiteRoutingService {

    SiteRoutingResult resolve(String contentPath);
}
