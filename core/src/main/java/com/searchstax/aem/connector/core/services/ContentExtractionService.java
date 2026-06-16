package com.searchstax.aem.connector.core.services;

import org.apache.sling.api.resource.Resource;

import java.util.Set;

public interface ContentExtractionService {

    Set<String> extractContent(Resource resource);
}
