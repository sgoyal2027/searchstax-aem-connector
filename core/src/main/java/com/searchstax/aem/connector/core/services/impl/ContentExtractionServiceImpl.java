package com.searchstax.aem.connector.core.services.impl;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.searchstax.aem.connector.core.services.ContentExtractionService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

@Component(service = ContentExtractionService.class)
public class ContentExtractionServiceImpl
        implements ContentExtractionService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    ContentExtractionServiceImpl.class);

    @Reference
    private IndexingHelperService indexingHelperService;

    @Override
    public Set<String> extractContent(
            Resource resource) {

        Set<String> content =
                new LinkedHashSet<>();

        Set<String> visitedPaths =
                new HashSet<>();

        collectContent(
                resource,
                content,
                visitedPaths);

        return content;
    }

    private void collectContent(
            Resource resource,
            Set<String> content,
            Set<String> visitedPaths) {

        if (resource == null) {
            return;
        }

        if (!visitedPaths.add(
                resource.getPath())) {

            return;
        }

        ValueMap valueMap =
                resource.getValueMap();

        valueMap.forEach((key, value) -> {

            if (shouldSkipProperty(key)) {
                return;
            }

            if (value instanceof String) {

                addContent(
                        content,
                        (String) value);

            } else if (value instanceof String[]) {

                for (String item :
                        (String[]) value) {

                    addContent(
                            content,
                            item);
                }
            }
        });

        processExperienceFragment(
                resource,
                content,
                visitedPaths);

        processContentFragment(
                resource,
                content);

        for (Resource child :
                resource.getChildren()) {

            collectContent(
                    child,
                    content,
                    visitedPaths);
        }
    }

    private void addContent(
            Set<String> content,
            String value) {

        String text =
                indexingHelperService
                        .cleanText(value);

        if (!isContentValue(text)) {
            return;
        }

        content.add(text);
    }

    private boolean shouldSkipProperty(
            String key) {

        return key.startsWith("jcr:")
                || key.startsWith("cq:")
                || key.startsWith("sling:")
                || "fileReference".equals(key)
                || "fragmentPath".equals(key)
                || "fragmentVariationPath".equals(key)
                || "resourceType".equals(key)
                || "allowedComponents".equals(key)
                || "dataLayer".equals(key)
                || "gridClassNames".equals(key)
                || "columnClassNames".equals(key)
                || "appliedCssClassNames".equals(key);
    }

    private boolean isContentValue(
            String value) {

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String normalized =
                value.trim();

        if ("true".equalsIgnoreCase(normalized)
                || "false".equalsIgnoreCase(normalized)) {

            return false;
        }

        if (normalized.matches("^\\d+$")) {
            return false;
        }

        if ("asc".equalsIgnoreCase(normalized)
                || "desc".equalsIgnoreCase(normalized)
                || "children".equalsIgnoreCase(normalized)
                || "responsiveGrid".equalsIgnoreCase(normalized)) {

            return false;
        }

        return normalized.length() > 2;
    }

    private void processExperienceFragment(
            Resource resource,
            Set<String> content,
            Set<String> visitedPaths) {

        String xfPath =
                resource.getValueMap()
                        .get(
                                "fragmentVariationPath",
                                String.class);

        if (xfPath == null || xfPath.trim().isEmpty()) {
            return;
        }

        Resource xfResource =
                resource.getResourceResolver()
                        .getResource(xfPath);

        if (xfResource == null) {
            return;
        }

        Resource xfContent =
                xfResource.getChild(
                        "jcr:content");

        if (xfContent == null) {
            return;
        }

        LOG.debug(
                "Processing Experience Fragment: {}",
                xfPath);

        collectContent(
                xfContent,
                content,
                visitedPaths);
    }

    private void processContentFragment(
            Resource resource,
            Set<String> content) {

        String fragmentPath =
                resource.getValueMap()
                        .get(
                                "fragmentPath",
                                String.class);

        if (fragmentPath == null || fragmentPath.trim().isEmpty()) {
            return;
        }

        Resource cfResource =
                resource.getResourceResolver()
                        .getResource(fragmentPath);

        if (cfResource == null) {
            return;
        }

        ContentFragment contentFragment =
                cfResource.adaptTo(
                        ContentFragment.class);

        if (contentFragment == null) {
            return;
        }

        LOG.debug(
                "Processing Content Fragment: {}",
                fragmentPath);

        Iterator<ContentElement> elements =
                contentFragment.getElements();

        while (elements.hasNext()) {

            ContentElement element =
                    elements.next();

            addContent(
                    content,
                    element.getContent());
        }
    }
}
