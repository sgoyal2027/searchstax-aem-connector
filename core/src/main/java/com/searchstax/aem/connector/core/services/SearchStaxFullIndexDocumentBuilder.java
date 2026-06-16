package com.searchstax.aem.connector.core.services;

import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds Solr update documents for full index using the same services and field
 * conventions as incremental indexing (PageDocumentBuilderServiceImpl / AssetDocumentBuilderServiceImpl).
 */
@Component(service = SearchStaxFullIndexDocumentBuilder.class)
public class SearchStaxFullIndexDocumentBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxFullIndexDocumentBuilder.class);

    private static final String DAM_PATH_PREFIX = SearchStaxFullIndexDefaults.DAM_ROOT + "/";

    @Reference
    private PageDocumentBuilderService pageDocumentBuilderService;

    @Reference
    private AssetDocumentBuilderService assetDocumentBuilderService;

    /**
     * @return document or {@code null} if not published, missing, or not a page
     */
    public Map<String, Object> buildPageIfPublished(final ResourceResolver resolver, final String path) {
        final Resource resource = resolver.getResource(path);
        if (resource == null) {
            LOG.debug("Page resource not found: {}", path);
            return null;
        }
        if (!isPublished(resource)) {
            LOG.debug("Skipping unpublished page: {}", path);
            return null;
        }
        final PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return null;
        }
        final Page page = pageManager.getPage(path);
        if (page == null || !path.equals(page.getPath())) {
            return null;
        }
        try {
            return pageDocumentBuilderService.buildDocument(resolver, path);
        } catch (Exception e) {
            LOG.warn("Failed to build page document for full index. Path={}", path, e);
            return null;
        }
    }

    /**
     * @return document or {@code null} if not published, missing, not a DAM asset, or unsupported type
     */
    public Map<String, Object> buildAssetIfPublished(final ResourceResolver resolver, final String path) {
        final Resource resource = resolver.getResource(path);
        if (resource == null) {
            LOG.debug("Asset resource not found: {}", path);
            return null;
        }
        if (!isDamAsset(resource)) {
            return null;
        }
        if (!isPublished(resource)) {
            LOG.debug("Skipping unpublished asset: {}", path);
            return null;
        }
        try {
            return assetDocumentBuilderService.buildDocument(resolver, path);
        } catch (Exception e) {
            LOG.warn("Failed to build asset document for full index. Path={}", path, e);
            return null;
        }
    }

    /**
     * Scans the page {@code jcr:content} tree for property values that reference DAM asset paths.
     * Does not perform publish checks; callers should invoke only after a page document was built successfully.
     *
     * @return deduplicated DAM paths (may be empty)
     */
    public Set<String> collectDamReferencesFromPage(final ResourceResolver resolver, final String pagePath) {
        final Set<String> references = new HashSet<>();
        if (resolver == null || pagePath == null || pagePath.isEmpty()) {
            return references;
        }
        final PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return references;
        }
        final Page page = pageManager.getPage(pagePath);
        if (page == null) {
            return references;
        }
        final Resource contentResource = page.getContentResource();
        if (contentResource != null) {
            collectDamReferences(contentResource, references);
        }
        return references;
    }

    static boolean isPublished(final Resource resource) {
        final ReplicationStatus status = resolveReplicationStatus(resource);
        return status != null
                && status.isActivated()
                && !status.isDeactivated()
                && !status.isPending();
    }

    static ReplicationStatus resolveReplicationStatus(final Resource resource) {
        if (resource == null) {
            return null;
        }
        ReplicationStatus status = resource.adaptTo(ReplicationStatus.class);
        if (status != null) {
            return status;
        }
        final Resource content = resource.getChild("jcr:content");
        if (content != null) {
            status = content.adaptTo(ReplicationStatus.class);
        }
        return status;
    }

    private static boolean isDamAsset(final Resource resource) {
        if (resource.getChild("jcr:content/metadata") == null) {
            return false;
        }
        final ValueMap vm = resource.getValueMap();
        return "dam:Asset".equals(vm.get("jcr:primaryType", String.class));
    }

    private static void collectDamReferences(final Resource resource, final Set<String> sink) {
        if (resource == null || sink == null) {
            return;
        }
        resource.getValueMap().forEach((key, value) -> addDamPathIfPresent(value, sink));
        for (final Resource child : resource.getChildren()) {
            collectDamReferences(child, sink);
        }
    }

    private static void addDamPathIfPresent(final Object value, final Set<String> sink) {
        if (value == null || sink == null) {
            return;
        }
        if (value instanceof String) {
            addDamPathString((String) value, sink);
        } else if (value instanceof String[]) {
            for (final String element : (String[]) value) {
                addDamPathString(element, sink);
            }
        } else if (value instanceof Object[]) {
            for (final Object element : (Object[]) value) {
                if (element != null) {
                    addDamPathString(element.toString(), sink);
                }
            }
        }
    }

    private static void addDamPathString(final String value, final Set<String> sink) {
        if (value == null) {
            return;
        }
        final String trimmed = value.trim();
        if (trimmed.startsWith(DAM_PATH_PREFIX)) {
            sink.add(trimmed);
        }
    }
}
