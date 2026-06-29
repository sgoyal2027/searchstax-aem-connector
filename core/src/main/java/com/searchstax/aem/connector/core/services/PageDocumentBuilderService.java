package com.searchstax.aem.connector.core.services;

import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;

public interface PageDocumentBuilderService {

        Map<String, Object> buildDocument(
            ResourceResolver resolver,
            String path);
    
}
