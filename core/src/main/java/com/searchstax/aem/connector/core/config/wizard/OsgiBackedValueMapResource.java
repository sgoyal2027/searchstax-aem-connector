package com.searchstax.aem.connector.core.config.wizard;

import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.LinkedHashMap;

/**
 * Synthetic {@link org.apache.sling.api.resource.Resource} that adapts to a fixed {@link ValueMap}
 * (used so Granite forms read OSGi-backed values).
 */
public final class OsgiBackedValueMapResource extends SyntheticResource {

    private final ValueMap valueMap;

    public OsgiBackedValueMapResource(
            final ResourceResolver resourceResolver,
            final String path,
            final String resourceType,
            final ValueMap valueMap) {
        super(resourceResolver, resourceMetadata(path), resourceType);
        this.valueMap = new ValueMapDecorator(new LinkedHashMap<>(valueMap));
    }

    private static ResourceMetadata resourceMetadata(final String path) {
        final ResourceMetadata metadata = new ResourceMetadata();
        metadata.setResolutionPath(path);
        return metadata;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) valueMap;
        }
        return super.adaptTo(type);
    }
}
