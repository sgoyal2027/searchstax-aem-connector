package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.Asset;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.services.AssetDocumentBuilderService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = AssetDocumentBuilderService.class)
public class AssetDocumentBuilderServiceImpl implements
        AssetDocumentBuilderService {

    private static final Logger LOG =
            LoggerFactory.getLogger(AssetDocumentBuilderServiceImpl.class);

    @Reference
    private Externalizer externalizer;

    @Reference
    private IndexingHelperService indexingHelperService;

    @Reference
    private LanguageConfigService languageConfigService;

    @Override
    public Map<String, Object> buildDocument(ResourceResolver resourceResolver, String path) throws Exception {

        LOG.debug(
                "Building SearchStax document for asset: {}",
                path);

        Resource assetResource =
                resourceResolver.getResource(path);

        if (assetResource == null) {

            throw new IllegalStateException(
                    "Asset resource not found for path: "
                            + path);
        }

        Asset asset =
                assetResource.adaptTo(Asset.class);

        if (asset == null) {

            throw new IllegalStateException(
                    "Unable to adapt resource to Asset. Path: "
                            + path);
        }

        if (!indexingHelperService
                .isSupportedAsset(asset)) {

            LOG.info(
                    "Skipping asset document creation. Asset type is not configured in Allowed Files. Path={}",
                    path);

            return null;
        }

        Map<String, Object> document =
                new HashMap<>();

        document.put("id", path);
        document.put("path", path);
        document.put("type", "asset");

        String assetUrl =
                externalizer.publishLink(
                        resourceResolver,
                        path);

        document.put("url", assetUrl);

        document.put(
                "mimeType",
                asset.getMimeType());

        document.put(
                "language_s",
                "en");

        Resource metadataResource =
                assetResource.getChild(
                        "jcr:content/metadata");

        if (metadataResource != null) {

            ValueMap metadata =
                    metadataResource.getValueMap();

            String metadataLanguage =
                    metadata.get(
                            "dc:language",
                            String.class);

            if (metadataLanguage != null
                    && !metadataLanguage.isBlank()) {

                document.put(
                        "language_s",
                        languageConfigService.mapToSearchStaxLanguage(metadataLanguage));
            }

            indexingHelperService
                    .addConfiguredMetadataFields(
                            document,
                            metadata);
        }

        String extractedText =
                indexingHelperService.cleanText(
                        indexingHelperService.extractText(
                                asset));

        String language = languageConfigService.mapToSearchStaxLanguage(
                (String) document.getOrDefault("language_s", "en"));
        document.put("language_s", language);

        indexingHelperService.addField(
                document,
                "content_txts_" + language,
                extractedText);

        LOG.debug(
                "Successfully built SearchStax document for asset: {}",
                path);

        return document;
    }
}
