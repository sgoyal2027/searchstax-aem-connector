package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.services.ContentExtractionService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import com.searchstax.aem.connector.core.services.PageDocumentBuilderService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = PageDocumentBuilderService.class)
public class PageDocumentBuilderServiceImpl
        implements PageDocumentBuilderService {

    private static final Logger LOG =
            LoggerFactory.getLogger(PageDocumentBuilderServiceImpl.class);

    @Reference
    private Externalizer externalizer;

    @Reference
    private IndexingHelperService indexingHelperService;

    @Reference
    private MetadataFieldConfigService metadataFieldConfigService;

    @Reference
    private LanguageConfigService languageConfigService;

    @Reference
    private ContentExtractionService contentExtractionService;

    @Override
    public Map<String, Object> buildDocument(ResourceResolver resolver, String path) {

        LOG.debug(
                "Building SearchStax document for page: {}",
                path);

        Resource pageResource =
                resolver.getResource(path);

        if (pageResource == null) {

            throw new IllegalStateException(
                    "Page resource not found for path: "
                            + path);
        }

        PageManager pageManager =
                resolver.adaptTo(PageManager.class);

        if (pageManager == null) {
            throw new IllegalStateException(
                    "Unable to adapt ResourceResolver to PageManager");
        }

        Page currentPage =
                pageManager.getContainingPage(pageResource);

        if (currentPage == null) {

            throw new IllegalStateException(
                    "Resource is not adaptable to Page. Path: "
                            + path);
        }

        Resource contentResource =
                currentPage.getContentResource();

        if (contentResource == null) {

            throw new IllegalStateException(
                    "Content resource not found for page: "
                            + path);
        }

        Locale locale =
                currentPage.getLanguage(false);

        String aemLanguage =
                locale != null
                        ? locale.getLanguage()
                        : "en";

        String language = languageConfigService.mapToSearchStaxLanguage(aemLanguage);

        Map<String, Object> document =
                new HashMap<>();

        buildPageDocument(
                resolver,
                currentPage,
                contentResource,
                document,
                language);

        LOG.debug(
                "Successfully built SearchStax document for page: {}",
                path);

        return document;
    }

    private void buildPageDocument(ResourceResolver resourceResolver,
                                 Page currentPage,
                                 Resource contentResource,
                                 Map<String, Object> document,
                                 String language) {

        String pagePath = currentPage.getPath();

        document.put("id", pagePath);

        String pageUrl =
                externalizer.publishLink(resourceResolver, pagePath)
                        + ".html";

        document.put("url", pageUrl);

        document.put("language_s", language);

        List<MetadataFieldMappingConfig> mappings =
                metadataFieldConfigService
                        .getMetadataFieldMappings();

        processMetadataFields(contentResource, document, language, mappings);

        Set<String> searchableContent =
                contentExtractionService
                        .extractContent(
                                contentResource);

        if (searchableContent == null) {

            searchableContent =
                    Collections.emptySet();
        }


        document.put("content_txts_"+language,
                new ArrayList<>(searchableContent));
    }

    private void processMetadataFields(
            Resource resource,
            Map<String, Object> document,
            String language,
            List<MetadataFieldMappingConfig> mappings) {

        if (resource == null) {
            return;
        }

        ValueMap valueMap =
                resource.getValueMap();

        // Add configured mapped fields
        for (MetadataFieldMappingConfig mapping : mappings) {

            if (!mapping.isEnabled()) {
                continue;
            }

            String sourceProperty =
                    (mapping.getCustomProperty() != null
                            && !mapping.getCustomProperty().trim().isEmpty())
                            ? mapping.getCustomProperty()
                            : mapping.getAemField();

            Object value =
                    valueMap.get(sourceProperty);

            if (value == null) {
                continue;
            }

            Object normalizedValue =
                    indexingHelperService
                            .normalizeMetadataValue(value);

            String fieldName =
                    indexingHelperService
                            .resolveFieldName(
                                    mapping.getSearchStaxField(),
                                    mapping.getSearchStaxFieldType(),
                                    value,
                                    language);

            indexingHelperService.addField(
                    document,
                    fieldName,
                    normalizedValue);
        }
    }
}
