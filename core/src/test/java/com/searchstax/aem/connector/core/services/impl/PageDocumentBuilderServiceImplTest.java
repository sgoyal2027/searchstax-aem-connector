package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.services.ContentExtractionService;
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
}