package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentValidationServiceImplTest {

    @Mock
    private MetadataFieldConfigService metadataFieldConfigService;

    @Mock
    private IndexingHelperService indexingHelperService;

    @InjectMocks
    private DocumentValidationServiceImpl validationService;

    @Test
    void validateMandatoryFields_passesWhenFieldPresent() {
        final MetadataFieldMappingConfig mapping = mandatoryMapping("title", "title_txt");
        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(List.of(mapping));
        when(indexingHelperService.resolveFieldName("title_txt", "text", "", "en")).thenReturn("title_txt_en");

        final Map<String, Object> document = Map.of("title_txt_en", "Hello");

        assertFalse(validationService.validateMandatoryFields(document, "en").isPresent());
    }

    @Test
    void validateMandatoryFields_failsWhenMandatoryFieldMissing() {
        final MetadataFieldMappingConfig mapping = mandatoryMapping("title", "title_txt");
        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(List.of(mapping));
        when(indexingHelperService.resolveFieldName("title_txt", "text", "", "en")).thenReturn("title_txt_en");

        assertTrue(validationService.validateMandatoryFields(Map.of("id", "/content/page"), "en").isPresent());
    }

    private static MetadataFieldMappingConfig mandatoryMapping(final String aemField, final String solrField) {
        final MetadataFieldMappingConfig config = new MetadataFieldMappingConfig();
        config.setEnabled(true);
        config.setMandatory(true);
        config.setAemField(aemField);
        config.setSearchStaxField(solrField);
        config.setSearchStaxFieldType("text");
        return config;
    }
}
