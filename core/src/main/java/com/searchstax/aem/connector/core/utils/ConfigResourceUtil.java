package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.HashMap;
import java.util.Map;

public final class ConfigResourceUtil {

    private ConfigResourceUtil() {
    }

    public static Resource getOrCreateConfigResource(
            final ResourceResolver resolver,
            final String configPath) throws PersistenceException {

        Resource resource = resolver.getResource(configPath);
        if (resource != null) {
            return resource;
        }

        final int lastSlash = configPath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return null;
        }

        final String parentPath = configPath.substring(0, lastSlash);
        final String nodeName = configPath.substring(lastSlash + 1);
        final Resource parent = resolver.getResource(parentPath);

        if (parent == null) {
            return null;
        }

        final Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", "sling:Folder");

        return resolver.create(parent, nodeName, properties);
    }

    public static ModifiableValueMap getModifiableProperties(final Resource resource) {
        if (resource == null) {
            return null;
        }
        return resource.adaptTo(ModifiableValueMap.class);
    }
}
