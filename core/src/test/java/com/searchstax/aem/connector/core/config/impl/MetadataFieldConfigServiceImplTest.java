package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataFieldConfigServiceImplTest {

    @InjectMocks
    private MetadataFieldConfigServiceImpl service;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;

    @Test
    void testActivate() throws Exception {

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);

        when(resource.getValueMap())
                .thenReturn(valueMap);

        String json =
                "[{\"aemField\":\"dc:title\","
                        + "\"customProperty\":\"\","
                        + "\"searchStaxField\":\"title_txt\","
                        + "\"searchStaxFieldType\":\"text\","
                        + "\"enabled\":true,"
                        + "\"mandatory\":false}]";

        when(valueMap.get(
                "metadataMappings",
                String.class))
                .thenReturn(json);

        service.activate();

        List<MetadataFieldMappingConfig> mappings =
                service.getMetadataFieldMappings();

        assertNotNull(mappings);
        assertEquals(1, mappings.size());

        MetadataFieldMappingConfig mapping =
                mappings.get(0);

        assertEquals("dc:title", mapping.getAemField());
        assertEquals("title_txt", mapping.getSearchStaxField());
        assertEquals("text", mapping.getSearchStaxFieldType());
        assertTrue(mapping.isEnabled());
        assertFalse(mapping.isMandatory());

        verify(resolver).close();
    }

    @Test
    void testGetMetadataFieldMappings() throws Exception {

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);

        when(resource.getValueMap())
                .thenReturn(valueMap);

        when(valueMap.get(
                "metadataMappings",
                String.class))
                .thenReturn(
                        "[{\"aemField\":\"dc:title\","
                                + "\"searchStaxField\":\"title_txt\"}]");

        service.activate();

        assertEquals(
                1,
                service.getMetadataFieldMappings().size());
    }

    @Test
    void testActivateWithNullResolverUtil() throws Exception {

        java.lang.reflect.Field field =
                MetadataFieldConfigServiceImpl.class
                        .getDeclaredField("resolverUtil");

        field.setAccessible(true);
        field.set(service, null);

        service.activate();

        assertTrue(
                service.getMetadataFieldMappings().isEmpty());
    }

    @Test
    void refreshMappings_reloadsWhenJsonChanges() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("metadataMappings", String.class))
                .thenReturn("[{\"aemField\":\"dc:title\",\"searchStaxField\":\"title_txt\"}]")
                .thenReturn(
                        "[{\"aemField\":\"dc:description\",\"searchStaxField\":\"description_txt\"},"
                                + "{\"aemField\":\"dc:title\",\"searchStaxField\":\"title_txt\"}]");

        service.activate();
        assertEquals(1, service.getMetadataFieldMappings().size());

        service.refreshMappings();

        assertEquals(2, service.getMetadataFieldMappings().size());
        assertEquals("dc:description", service.getMetadataFieldMappings().get(0).getAemField());
    }

    @Test
    void loadMappings_returnsEmptyWhenConfigResourceMissing() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(null);

        service.activate();

        assertTrue(service.getMetadataFieldMappings().isEmpty());
    }

    @Test
    void loadMappings_returnsEmptyWhenJsonMissingOrBlank() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("metadataMappings", String.class)).thenReturn("   ");

        service.activate();

        assertTrue(service.getMetadataFieldMappings().isEmpty());
    }

    @Test
    void loadMappings_returnsEmptyWhenJsonInvalid() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/metadatafieldmapping"))
                .thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("metadataMappings", String.class)).thenReturn("{not-json}");

        service.activate();

        assertTrue(service.getMetadataFieldMappings().isEmpty());
    }

    @Test
    void loadMappings_returnsEmptyWhenResolverThrows() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("resolver unavailable"));

        service.activate();

        assertTrue(service.getMetadataFieldMappings().isEmpty());
    }
}