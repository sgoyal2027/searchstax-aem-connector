package com.searchstax.aem.connector.core.services;

import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchStaxFullIndexDocumentBuilderTest {

    private static final String ASSET_PATH = "/content/dam/wknd/us/sample.jpg";
    private static final String PAGE_PATH = "/content/wknd/us/en/test";

    @Mock
    private PageDocumentBuilderService pageDocumentBuilderService;

    @Mock
    private AssetDocumentBuilderService assetDocumentBuilderService;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource assetResource;

    @Mock
    private Resource pageResource;

    @Mock
    private ReplicationStatus replicationStatus;

    @Mock
    private PageManager pageManager;

    @Mock
    private Page page;

    @InjectMocks
    private SearchStaxFullIndexDocumentBuilder documentBuilder;

    @BeforeEach
    void setUp() throws Exception {
        injectField("pageDocumentBuilderService", pageDocumentBuilderService);
        injectField("assetDocumentBuilderService", assetDocumentBuilderService);
    }

    @Test
    void buildAssetIfPublished_delegatesToAssetDocumentBuilder() throws Exception {
        stubPublishedAsset();
        final Map<String, Object> expected = new HashMap<>();
        expected.put("id", ASSET_PATH);
        expected.put("type", "asset");
        expected.put("content_txts_en", "extracted text");
        when(assetDocumentBuilderService.buildDocument(resolver, ASSET_PATH)).thenReturn(expected);

        final Map<String, Object> document = documentBuilder.buildAssetIfPublished(resolver, ASSET_PATH);

        assertNotNull(document);
        assertEquals(ASSET_PATH, document.get("id"));
        verify(assetDocumentBuilderService).buildDocument(resolver, ASSET_PATH);
    }

    @Test
    void buildPageIfPublished_delegatesToPageDocumentBuilder() throws Exception {
        stubPublishedPage();
        final Map<String, Object> expected = new HashMap<>();
        expected.put("id", PAGE_PATH);
        expected.put("language_s", "en");
        when(pageDocumentBuilderService.buildDocument(resolver, PAGE_PATH)).thenReturn(expected);

        final Map<String, Object> document = documentBuilder.buildPageIfPublished(resolver, PAGE_PATH);

        assertNotNull(document);
        assertEquals(PAGE_PATH, document.get("id"));
        verify(pageDocumentBuilderService).buildDocument(resolver, PAGE_PATH);
    }

    @Test
    void buildAssetIfPublished_returnsNullForUnsupportedAssetType() throws Exception {
        stubPublishedAsset();
        when(assetDocumentBuilderService.buildDocument(resolver, ASSET_PATH)).thenReturn(null);

        assertNull(documentBuilder.buildAssetIfPublished(resolver, ASSET_PATH));
    }

    @Test
    void collectDamReferencesFromPage_findsNestedFileReferenceAndIgnoresNonDamPaths() {
        final String pagePath = "/content/wknd/us/en/magazine";
        final String damRef = "/content/dam/wknd/us/hero.jpg";
        final Resource contentResource = mock(Resource.class);
        final Resource childComponent = mock(Resource.class);
        final ValueMap contentValueMap = mock(ValueMap.class);
        final ValueMap childValueMap = mock(ValueMap.class);

        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage(pagePath)).thenReturn(page);
        when(page.getContentResource()).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(contentValueMap);
        when(contentResource.getChildren()).thenReturn(Collections.singletonList(childComponent));
        when(childComponent.getValueMap()).thenReturn(childValueMap);
        when(childComponent.getChildren()).thenReturn(Collections.emptyList());
        stubValueMapForEach(
                contentValueMap,
                consumer -> consumer.accept("jcr:title", "Magazine"));
        stubValueMapForEach(
                childValueMap,
                consumer -> {
                    consumer.accept("fileReference", damRef);
                    consumer.accept("link", "/content/wknd/us/en/other");
                });

        final Set<String> references = documentBuilder.collectDamReferencesFromPage(resolver, pagePath);

        assertEquals(Set.of(damRef), references);
    }

    @Test
    void collectDamReferencesFromPage_collectsMultiValueDamPaths() {
        final String pagePath = "/content/wknd/us/en/page";
        final Resource contentResource = mock(Resource.class);
        final ValueMap contentValueMap = mock(ValueMap.class);

        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage(pagePath)).thenReturn(page);
        when(page.getContentResource()).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(contentValueMap);
        stubValueMapForEach(
                contentValueMap,
                consumer ->
                        consumer.accept(
                                "assets",
                                new String[] {
                                    "/content/dam/wknd/a.jpg", "/content/dam/wknd/b.jpg", "/content/dam/wknd/a.jpg"
                                }));

        final Set<String> references = documentBuilder.collectDamReferencesFromPage(resolver, pagePath);

        assertEquals(
                Set.of("/content/dam/wknd/a.jpg", "/content/dam/wknd/b.jpg"),
                references);
    }

    @Test
    void collectDamReferencesFromPage_returnsEmptyWhenPageMissing() {
        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage("/content/missing")).thenReturn(null);

        assertTrue(documentBuilder.collectDamReferencesFromPage(resolver, "/content/missing").isEmpty());
    }

    @Test
    void buildAssetIfPublished_returnsNullWhenUnpublished() {
        when(resolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.getChild("jcr:content/metadata")).thenReturn(mock(Resource.class));
        when(assetResource.getValueMap()).thenReturn(mock(ValueMap.class));
        when(assetResource.getValueMap().get("jcr:primaryType", String.class)).thenReturn("dam:Asset");
        when(assetResource.adaptTo(ReplicationStatus.class)).thenReturn(replicationStatus);
        when(replicationStatus.isActivated()).thenReturn(false);

        assertNull(documentBuilder.buildAssetIfPublished(resolver, ASSET_PATH));
    }

    private void stubPublishedPage() {
        when(resolver.getResource(PAGE_PATH)).thenReturn(pageResource);
        when(pageResource.adaptTo(ReplicationStatus.class)).thenReturn(replicationStatus);
        when(replicationStatus.isActivated()).thenReturn(true);
        when(replicationStatus.isDeactivated()).thenReturn(false);
        when(replicationStatus.isPending()).thenReturn(false);
        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage(PAGE_PATH)).thenReturn(page);
        when(page.getPath()).thenReturn(PAGE_PATH);
    }

    private void stubPublishedAsset() {
        when(resolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.getChild("jcr:content/metadata")).thenReturn(mock(Resource.class));
        final ValueMap valueMap = mock(ValueMap.class);
        when(assetResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("jcr:primaryType", String.class)).thenReturn("dam:Asset");
        when(assetResource.adaptTo(ReplicationStatus.class)).thenReturn(replicationStatus);
        when(replicationStatus.isActivated()).thenReturn(true);
        when(replicationStatus.isDeactivated()).thenReturn(false);
        when(replicationStatus.isPending()).thenReturn(false);
    }

    private static void stubValueMapForEach(
            final ValueMap valueMap, final java.util.function.Consumer<BiConsumer<String, Object>> setup) {
        doAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            final BiConsumer<String, Object> consumer = invocation.getArgument(0);
                            setup.accept(consumer);
                            return null;
                        })
                .when(valueMap)
                .forEach(any());
    }

    private void injectField(final String name, final Object value) throws Exception {
        final Field field = SearchStaxFullIndexDocumentBuilder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(documentBuilder, value);
    }
}
