package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.Asset;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetDocumentBuilderServiceImplTest {

    @InjectMocks
    private AssetDocumentBuilderServiceImpl service;

    @Mock
    private Externalizer externalizer;

    @Mock
    private IndexingHelperService indexingHelperService;

    @Mock
    private LanguageConfigService languageConfigService;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource assetResource;

    @Mock
    private Resource metadataResource;

    @Mock
    private Asset asset;

    @Mock
    private ValueMap metadata;

    @Test
    void testBuildDocumentResourceNotFound() {

        when(resolver.getResource("/content/dam/test.pdf"))
                .thenReturn(null);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.buildDocument(
                                resolver,
                                "/content/dam/test.pdf"));

        assertTrue(exception.getMessage().contains("Asset resource not found"));
    }

    @Test
    void testBuildDocumentAssetAdaptFailed() {

        when(resolver.getResource("/content/dam/test.pdf"))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(null);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.buildDocument(
                                resolver,
                                "/content/dam/test.pdf"));

        assertTrue(exception.getMessage().contains("Unable to adapt resource to Asset"));
    }

    @Test
    void testBuildDocumentUnsupportedAsset() throws Exception {

        when(resolver.getResource("/content/dam/test.pdf"))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(asset);

        when(indexingHelperService.isSupportedAsset(asset))
                .thenReturn(false);

        Map<String, Object> result =
                service.buildDocument(
                        resolver,
                        "/content/dam/test.pdf");

        assertNull(result);

        verify(indexingHelperService)
                .isSupportedAsset(asset);
    }

    @Test
    void testBuildDocumentWithoutMetadata() throws Exception {

        when(resolver.getResource("/content/dam/test.pdf"))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(asset);

        when(indexingHelperService.isSupportedAsset(asset))
                .thenReturn(true);

        when(externalizer.publishLink(
                resolver,
                "/content/dam/test.pdf"))
                .thenReturn("https://publish/content/dam/test.pdf");

        when(asset.getMimeType())
                .thenReturn("application/pdf");

        when(assetResource.getChild("jcr:content/metadata"))
                .thenReturn(null);

        when(indexingHelperService.extractText(asset))
                .thenReturn("sample text");

        when(indexingHelperService.cleanText("sample text"))
                .thenReturn("sample text");

        when(languageConfigService.mapToSearchStaxLanguage("en"))
                .thenReturn("en");

        when(languageConfigService.resolveLanguageFromPath("/content/dam/test.pdf"))
                .thenReturn("en");

        Map<String, Object> document =
                service.buildDocument(
                        resolver,
                        "/content/dam/test.pdf");

        assertNotNull(document);
        assertEquals("/content/dam/test.pdf", document.get("id"));
        assertEquals("asset", document.get("type"));
        assertEquals("application/pdf", document.get("mimeType"));
        assertEquals("https://publish/content/dam/test.pdf", document.get("url"));
        assertEquals("en", document.get("language_s"));

        verify(indexingHelperService)
                .addField(
                        eq(document),
                        eq("content_txts_en"),
                        eq("sample text"));
    }

    @Test
    void testBuildDocumentWithMetadataLanguage() throws Exception {

        when(resolver.getResource("/content/dam/test.pdf"))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(asset);

        when(indexingHelperService.isSupportedAsset(asset))
                .thenReturn(true);

        when(asset.getMimeType())
                .thenReturn("application/pdf");

        when(externalizer.publishLink(any(), anyString()))
                .thenReturn("https://publish/content/dam/test.pdf");

        when(assetResource.getChild("jcr:content/metadata"))
                .thenReturn(metadataResource);

        when(metadataResource.getValueMap())
                .thenReturn(metadata);

        when(metadata.get("dc:language", String.class))
                .thenReturn("fr");

        when(languageConfigService.mapToSearchStaxLanguage("fr"))
                .thenReturn("fr");

        when(indexingHelperService.extractText(asset))
                .thenReturn("bonjour");

        when(indexingHelperService.cleanText("bonjour"))
                .thenReturn("bonjour");

        Map<String, Object> document =
                service.buildDocument(
                        resolver,
                        "/content/dam/test.pdf");

        assertEquals("fr", document.get("language_s"));

        verify(indexingHelperService)
                .addConfiguredMetadataFields(
                        eq(document),
                        eq(metadata));

        verify(indexingHelperService)
                .addField(
                        eq(document),
                        eq("content_txts_fr"),
                        eq("bonjour"));
    }

    @Test
    void testBuildDocumentBlankMetadataLanguage() throws Exception {

        when(resolver.getResource("/content/dam/test.pdf"))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(asset);

        when(indexingHelperService.isSupportedAsset(asset))
                .thenReturn(true);

        when(asset.getMimeType())
                .thenReturn("application/pdf");

        when(externalizer.publishLink(any(), anyString()))
                .thenReturn("https://publish/content/dam/test.pdf");

        when(assetResource.getChild("jcr:content/metadata"))
                .thenReturn(metadataResource);

        when(metadataResource.getValueMap())
                .thenReturn(metadata);

        when(metadata.get("dc:language", String.class))
                .thenReturn("");

        when(languageConfigService.resolveLanguageFromPath("/content/dam/test.pdf"))
                .thenReturn("en");

        when(languageConfigService.mapToSearchStaxLanguage("en"))
                .thenReturn("en");

        when(indexingHelperService.extractText(asset))
                .thenReturn("hello");

        when(indexingHelperService.cleanText("hello"))
                .thenReturn("hello");

        Map<String, Object> document =
                service.buildDocument(
                        resolver,
                        "/content/dam/test.pdf");

        assertEquals("en", document.get("language_s"));
    }

    @Test
    void testBuildDocumentWithPathLanguage() throws Exception {

        final String assetPath = "/content/dam/wknd/fr/report.pdf";

        when(resolver.getResource(assetPath))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(asset);

        when(indexingHelperService.isSupportedAsset(asset))
                .thenReturn(true);

        when(asset.getMimeType())
                .thenReturn("application/pdf");

        when(externalizer.publishLink(any(), anyString()))
                .thenReturn("https://publish/content/dam/wknd/fr/report.pdf");

        when(assetResource.getChild("jcr:content/metadata"))
                .thenReturn(null);

        when(languageConfigService.resolveLanguageFromPath(assetPath))
                .thenReturn("fr");

        when(languageConfigService.mapToSearchStaxLanguage("fr"))
                .thenReturn("fr");

        when(indexingHelperService.extractText(asset))
                .thenReturn("bonjour");

        when(indexingHelperService.cleanText("bonjour"))
                .thenReturn("bonjour");

        Map<String, Object> document =
                service.buildDocument(
                        resolver,
                        assetPath);

        assertEquals("fr", document.get("language_s"));

        verify(indexingHelperService)
                .addField(
                        eq(document),
                        eq("content_txts_fr"),
                        eq("bonjour"));
    }

    @Test
    void testBuildDocumentWithFullLanguageNameInPath() throws Exception {

        final String assetPath = "/content/dam/wknd/english/report.pdf";

        when(resolver.getResource(assetPath))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(asset);

        when(indexingHelperService.isSupportedAsset(asset))
                .thenReturn(true);

        when(asset.getMimeType())
                .thenReturn("application/pdf");

        when(externalizer.publishLink(any(), anyString()))
                .thenReturn("https://publish/content/dam/wknd/english/report.pdf");

        when(assetResource.getChild("jcr:content/metadata"))
                .thenReturn(null);

        when(languageConfigService.resolveLanguageFromPath(assetPath))
                .thenReturn("en");

        when(languageConfigService.mapToSearchStaxLanguage("en"))
                .thenReturn("en");

        when(indexingHelperService.extractText(asset))
                .thenReturn("hello");

        when(indexingHelperService.cleanText("hello"))
                .thenReturn("hello");

        Map<String, Object> document =
                service.buildDocument(
                        resolver,
                        assetPath);

        assertEquals("en", document.get("language_s"));

        verify(indexingHelperService)
                .addField(
                        eq(document),
                        eq("content_txts_en"),
                        eq("hello"));
    }

    @Test
    void testBuildDocumentMetadataLanguageOverridesPath() throws Exception {

        final String assetPath = "/content/dam/wknd/en/report.pdf";

        when(resolver.getResource(assetPath))
                .thenReturn(assetResource);

        when(assetResource.adaptTo(Asset.class))
                .thenReturn(asset);

        when(indexingHelperService.isSupportedAsset(asset))
                .thenReturn(true);

        when(asset.getMimeType())
                .thenReturn("application/pdf");

        when(externalizer.publishLink(any(), anyString()))
                .thenReturn("https://publish/content/dam/wknd/en/report.pdf");

        when(assetResource.getChild("jcr:content/metadata"))
                .thenReturn(metadataResource);

        when(metadataResource.getValueMap())
                .thenReturn(metadata);

        when(metadata.get("dc:language", String.class))
                .thenReturn("fr");

        when(languageConfigService.mapToSearchStaxLanguage("fr"))
                .thenReturn("fr");

        when(indexingHelperService.extractText(asset))
                .thenReturn("bonjour");

        when(indexingHelperService.cleanText("bonjour"))
                .thenReturn("bonjour");

        Map<String, Object> document =
                service.buildDocument(
                        resolver,
                        assetPath);

        assertEquals("fr", document.get("language_s"));

        verify(languageConfigService, never())
                .resolveLanguageFromPath(anyString());
    }
}