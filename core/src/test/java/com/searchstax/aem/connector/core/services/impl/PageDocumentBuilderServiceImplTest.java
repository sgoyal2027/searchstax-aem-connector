package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.services.ContentExtractionService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageDocumentBuilderServiceImplTest {

    @InjectMocks
    private PageDocumentBuilderServiceImpl service;

    @Mock
    private Externalizer externalizer;

    @Mock
    private MetadataFieldConfigService metadataFieldConfigService;

    @Mock
    private LanguageConfigService languageConfigService;

    @Mock
    private ContentExtractionService contentExtractionService;

    @Mock
    private IndexingHelperService indexingHelperService;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource pageResource;

    @Mock
    private Resource contentResource;

    @Mock
    private PageManager pageManager;

    @Mock
    private Page page;

    @Mock
    private ValueMap valueMap;

    private void mockHappyPath() {

        when(resolver.getResource("/content/site"))
                .thenReturn(pageResource);

        when(resolver.adaptTo(PageManager.class))
                .thenReturn(pageManager);

        when(pageManager.getContainingPage(pageResource))
                .thenReturn(page);

        when(page.getContentResource())
                .thenReturn(contentResource);

        when(page.getPath())
                .thenReturn("/content/site");

        when(page.getLanguage(false))
                .thenReturn(Locale.ENGLISH);

        when(contentResource.getValueMap())
                .thenReturn(valueMap);

        when(languageConfigService.mapToSearchStaxLanguage("en"))
                .thenReturn("en");

        when(metadataFieldConfigService.getMetadataFieldMappings())
                .thenReturn(Collections.emptyList());

        when(contentExtractionService.extractContent(contentResource))
                .thenReturn(Set.of("Hello", "World"));

        when(externalizer.publishLink(resolver, "/content/site"))
                .thenReturn("https://publish/content/site");
    }

    @Test
    void testBuildDocumentSuccess() throws Exception {

        mockHappyPath();

        Map<String,Object> document =
                service.buildDocument(resolver,"/content/site");

        assertNotNull(document);
        assertEquals("/content/site",document.get("id"));
        assertEquals(
                "https://publish/content/site.html",
                document.get("url"));
        assertEquals("en",document.get("language_s"));
        assertTrue(document.containsKey("content_txts_en"));
    }

    @Test
    void testPageResourceNotFound() {

        when(resolver.getResource("/content/site"))
                .thenReturn(null);

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.buildDocument(resolver,"/content/site"));

        assertTrue(ex.getMessage().contains("Page resource not found"));
    }

    @Test
    void testPageManagerNull() {

        when(resolver.getResource("/content/site"))
                .thenReturn(pageResource);

        when(resolver.adaptTo(PageManager.class))
                .thenReturn(null);

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.buildDocument(resolver,"/content/site"));

        assertTrue(ex.getMessage().contains("PageManager"));
    }

    @Test
    void testContainingPageNull() {

        when(resolver.getResource("/content/site"))
                .thenReturn(pageResource);

        when(resolver.adaptTo(PageManager.class))
                .thenReturn(pageManager);

        when(pageManager.getContainingPage(pageResource))
                .thenReturn(null);

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.buildDocument(resolver,"/content/site"));

        assertTrue(ex.getMessage().contains("Resource is not adaptable"));
    }

    @Test
    void testContentResourceNull() {

        when(resolver.getResource("/content/site"))
                .thenReturn(pageResource);

        when(resolver.adaptTo(PageManager.class))
                .thenReturn(pageManager);

        when(pageManager.getContainingPage(pageResource))
                .thenReturn(page);

        when(page.getContentResource())
                .thenReturn(null);

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.buildDocument(resolver,"/content/site"));

        assertTrue(ex.getMessage().contains("Content resource not found"));
    }

    @Test
    void buildDocument_appliesEnabledMetadataMappings() throws Exception {
        mockHappyPath();

        final MetadataFieldMappingConfig enabledMapping = new MetadataFieldMappingConfig();
        enabledMapping.setEnabled(true);
        enabledMapping.setAemField("jcr:title");
        enabledMapping.setSearchStaxField("title");
        enabledMapping.setSearchStaxFieldType("text");

        final MetadataFieldMappingConfig disabledMapping = new MetadataFieldMappingConfig();
        disabledMapping.setEnabled(false);
        disabledMapping.setAemField("jcr:description");
        disabledMapping.setSearchStaxField("description");
        disabledMapping.setSearchStaxFieldType("text");

        when(metadataFieldConfigService.getMetadataFieldMappings())
                .thenReturn(List.of(enabledMapping, disabledMapping));
        when(valueMap.get("jcr:title")).thenReturn("Page Title");
        when(indexingHelperService.normalizeMetadataValue("Page Title")).thenReturn("Page Title");
        when(indexingHelperService.resolveFieldName("title", "text", "Page Title", "en"))
                .thenReturn("title_txt_en");

        final Map<String, Object> document = service.buildDocument(resolver, "/content/site");

        verify(indexingHelperService).addField(document, "title_txt_en", "Page Title");
        verify(indexingHelperService, never()).addField(eq(document), eq("description_txt_en"), any());
    }

    @Test
    void buildDocument_usesCustomPropertyWhenConfigured() throws Exception {
        mockHappyPath();

        final MetadataFieldMappingConfig mapping = new MetadataFieldMappingConfig();
        mapping.setEnabled(true);
        mapping.setAemField("jcr:title");
        mapping.setCustomProperty("navTitle");
        mapping.setSearchStaxField("nav");
        mapping.setSearchStaxFieldType("text");

        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(List.of(mapping));
        when(valueMap.get("navTitle")).thenReturn("Nav Label");
        when(indexingHelperService.normalizeMetadataValue("Nav Label")).thenReturn("Nav Label");
        when(indexingHelperService.resolveFieldName("nav", "text", "Nav Label", "en"))
                .thenReturn("nav_txt_en");

        service.buildDocument(resolver, "/content/site");

        verify(valueMap).get("navTitle");
        verify(indexingHelperService).addField(any(), eq("nav_txt_en"), eq("Nav Label"));
    }

    @Test
    void buildDocument_defaultsLanguageWhenLocaleMissing() throws Exception {
        mockHappyPath();
        when(page.getLanguage(false)).thenReturn(null);
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn("en");

        final Map<String, Object> document = service.buildDocument(resolver, "/content/site");

        assertEquals("en", document.get("language_s"));
        assertTrue(document.containsKey("content_txts_en"));
    }

    @Test
    void buildDocument_handlesNullExtractedContent() throws Exception {
        mockHappyPath();
        when(contentExtractionService.extractContent(contentResource)).thenReturn(null);

        final Map<String, Object> document = service.buildDocument(resolver, "/content/site");

        assertEquals(List.of(), document.get("content_txts_en"));
    }
}