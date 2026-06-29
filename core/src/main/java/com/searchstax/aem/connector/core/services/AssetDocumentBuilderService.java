package com.searchstax.aem.connector.core.services;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;

public interface AssetDocumentBuilderService {

    Map<String, Object> buildDocument(
            ResourceResolver resourceResolver,
            String path);
}
