package com.searchstax.aem.connector.core.services.impl;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentExtractionServiceImplTest {

    @InjectMocks
    private ContentExtractionServiceImpl service;

    @Mock
    private IndexingHelperService indexingHelperService;

    @Mock
    private Resource resource;

    private void mockBasicResource(ValueMap valueMap) {

        when(resource.getPath())
                .thenReturn("/content/site");

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(resource.getChildren())
                .thenReturn(Collections.emptyList());
    }

    @Test
    void testExtractContentNullResource() {

        Set<String> result = service.extractContent(null);

        assertTrue(result.isEmpty());

        verifyNoInteractions(indexingHelperService);
    }

    @Test
    void testExtractSimpleString() {

        Map<String, Object> props = new HashMap<>();
        props.put("title", "Hello World");

        ValueMap valueMap = new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result = service.extractContent(resource);

        assertEquals(1, result.size());
        assertTrue(result.contains("Hello World"));
    }

    @Test
    void testExtractStringArray() {

        Map<String, Object> props = new HashMap<>();
        props.put("tags", new String[]{
                "Java",
                "AEM"
        });

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result =
                service.extractContent(resource);

        assertEquals(2, result.size());

        assertTrue(result.contains("Java"));
        assertTrue(result.contains("AEM"));
    }

    @Test
    void testSkipJcrProperties() {

        Map<String, Object> props = new HashMap<>();

        props.put("jcr:title", "Ignore");

        props.put("title", "Include");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result =
                service.extractContent(resource);

        assertEquals(1, result.size());

        assertTrue(result.contains("Include"));
    }

    @Test
    void testSkipBooleanValue() {

        Map<String, Object> props = new HashMap<>();

        props.put("enabled", "true");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSkipNumericValue() {

        Map<String, Object> props = new HashMap<>();

        props.put("count", "12345");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSkipShortValue() {

        Map<String, Object> props = new HashMap<>();

        props.put("title", "Hi");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExperienceFragmentNotFound() {

        Map<String, Object> props = new HashMap<>();
        props.put("fragmentVariationPath",
                "/content/experience-fragments/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/experience-fragments/test"))
                .thenReturn(null);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExperienceFragmentContentMissing() {

        Map<String, Object> props = new HashMap<>();
        props.put("fragmentVariationPath",
                "/content/experience-fragments/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        Resource xfResource =
                mock(Resource.class);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/experience-fragments/test"))
                .thenReturn(xfResource);

        when(xfResource.getChild("jcr:content"))
                .thenReturn(null);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testContentFragmentNotFound() {

        Map<String, Object> props = new HashMap<>();
        props.put("fragmentPath",
                "/content/dam/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/dam/test"))
                .thenReturn(null);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testContentFragmentAdaptReturnsNull() {

        Map<String, Object> props = new HashMap<>();
        props.put("fragmentPath",
                "/content/dam/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        Resource cfResource =
                mock(Resource.class);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/dam/test"))
                .thenReturn(cfResource);

        when(cfResource.adaptTo(ContentFragment.class))
                .thenReturn(null);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testContentFragmentSuccess() {

        Map<String, Object> props = new HashMap<>();
        props.put("fragmentPath",
                "/content/dam/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        Resource cfResource =
                mock(Resource.class);

        ContentFragment fragment =
                mock(ContentFragment.class);

        ContentElement element =
                mock(ContentElement.class);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/dam/test"))
                .thenReturn(cfResource);

        when(cfResource.adaptTo(ContentFragment.class))
                .thenReturn(fragment);

        when(fragment.getElements())
                .thenReturn(Collections.singletonList(element).iterator());

        when(element.getContent())
                .thenReturn("Fragment Content");

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result =
                service.extractContent(resource);

        assertEquals(1, result.size());

        assertTrue(result.contains("Fragment Content"));
    }

    @Test
    void testContentFragmentMultipleElements() {

        Map<String, Object> props = new HashMap<>();
        props.put("fragmentPath",
                "/content/dam/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        Resource cfResource =
                mock(Resource.class);

        ContentFragment fragment =
                mock(ContentFragment.class);

        ContentElement element1 =
                mock(ContentElement.class);

        ContentElement element2 =
                mock(ContentElement.class);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/dam/test"))
                .thenReturn(cfResource);

        when(cfResource.adaptTo(ContentFragment.class))
                .thenReturn(fragment);

        when(fragment.getElements())
                .thenReturn(Arrays.asList(element1, element2).iterator());

        when(element1.getContent())
                .thenReturn("Content One");

        when(element2.getContent())
                .thenReturn("Content Two");

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result =
                service.extractContent(resource);

        assertEquals(2, result.size());

        assertTrue(result.contains("Content One"));
        assertTrue(result.contains("Content Two"));
    }

    @Test
    void testDuplicateResourceIgnored() {

        Map<String, Object> props = new HashMap<>();

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        Resource child =
                mock(Resource.class);

        when(resource.getChildren())
                .thenReturn(Collections.singletonList(child));

        // Same path as parent
        when(child.getPath())
                .thenReturn("/content/site");

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());

        // Child should never be processed because the path was already visited
        verify(child, never()).getValueMap();
    }

    @Test
    void testChildResourcesProcessed() {

        Map<String, Object> props = new HashMap<>();

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        Resource child =
                mock(Resource.class);

        Map<String, Object> childProps =
                new HashMap<>();

        childProps.put("title", "Child Content");

        ValueMap childValueMap =
                new ValueMapDecorator(childProps);

        when(resource.getChildren())
                .thenReturn(Collections.singletonList(child));

        when(child.getPath())
                .thenReturn("/content/site/child");

        when(child.getValueMap())
                .thenReturn(childValueMap);

        when(child.getChildren())
                .thenReturn(Collections.emptyList());

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result =
                service.extractContent(resource);

        assertEquals(1, result.size());

        assertTrue(result.contains("Child Content"));
    }

    @Test
    void testExperienceFragmentProcessed() {

        Map<String, Object> props =
                new HashMap<>();

        props.put(
                "fragmentVariationPath",
                "/content/experience-fragments/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        Resource xfResource =
                mock(Resource.class);

        Resource xfContent =
                mock(Resource.class);

        Map<String, Object> xfProps =
                new HashMap<>();

        xfProps.put(
                "title",
                "XF Content");

        ValueMap xfValueMap =
                new ValueMapDecorator(xfProps);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/experience-fragments/test"))
                .thenReturn(xfResource);

        when(xfResource.getChild("jcr:content"))
                .thenReturn(xfContent);

        when(xfContent.getPath())
                .thenReturn("/content/experience-fragments/test/jcr:content");

        when(xfContent.getValueMap())
                .thenReturn(xfValueMap);

        when(xfContent.getChildren())
                .thenReturn(Collections.emptyList());

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result =
                service.extractContent(resource);

        assertEquals(1, result.size());

        assertTrue(result.contains("XF Content"));
    }

    @Test
    void testContentFragmentEmptyElements() {

        Map<String, Object> props =
                new HashMap<>();

        props.put(
                "fragmentPath",
                "/content/dam/test");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        ResourceResolver resolver =
                mock(ResourceResolver.class);

        Resource cfResource =
                mock(Resource.class);

        ContentFragment fragment =
                mock(ContentFragment.class);

        when(resource.getResourceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content/dam/test"))
                .thenReturn(cfResource);

        when(cfResource.adaptTo(ContentFragment.class))
                .thenReturn(fragment);

        when(fragment.getElements())
                .thenReturn(Collections.emptyIterator());

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCleanTextReturnsNull() {

        Map<String, Object> props =
                new HashMap<>();

        props.put(
                "title",
                "Original");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        when(resource.getPath())
                .thenReturn("/content/site");

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(resource.getChildren())
                .thenReturn(Collections.emptyList());

        when(indexingHelperService.cleanText(anyString()))
                .thenReturn(null);

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCleanTextReturnsBlank() {

        Map<String, Object> props =
                new HashMap<>();

        props.put(
                "title",
                "Original");

        ValueMap valueMap =
                new ValueMapDecorator(props);

        when(resource.getPath())
                .thenReturn("/content/site");

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(resource.getChildren())
                .thenReturn(Collections.emptyList());

        when(indexingHelperService.cleanText(anyString()))
                .thenReturn("   ");

        Set<String> result =
                service.extractContent(resource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testDuplicateContentStoredOnlyOnce() {

        Map<String, Object> props =
                new HashMap<>();

        props.put(
                "title",
                new String[]{
                        "Java",
                        "Java"
                });

        ValueMap valueMap =
                new ValueMapDecorator(props);

        mockBasicResource(valueMap);

        when(indexingHelperService.cleanText(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result =
                service.extractContent(resource);

        assertEquals(1, result.size());

        assertTrue(result.contains("Java"));
    }
}