package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.services.DocumentValidationService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component(service = DocumentValidationService.class)
public class DocumentValidationServiceImpl implements DocumentValidationService {

    @Reference
    private MetadataFieldConfigService metadataFieldConfigService;

    @Reference
    private IndexingHelperService indexingHelperService;

    @Override
    public Optional<String> validateMandatoryFields(
            final Map<String, Object> document, final String language) {

        if (document == null) {
            return Optional.of("MISSING_MANDATORY_FIELD: document is null");
        }

        final String lang = language != null && !language.isBlank() ? language : "en";
        final List<MetadataFieldMappingConfig> mappings = metadataFieldConfigService.getMetadataFieldMappings();

        for (final MetadataFieldMappingConfig mapping : mappings) {
            if (!mapping.isEnabled() || !mapping.isMandatory()) {
                continue;
            }

            final String fieldName = indexingHelperService.resolveFieldName(
                    mapping.getSearchStaxField(),
                    mapping.getSearchStaxFieldType(),
                    "",
                    lang);

            if (!document.containsKey(fieldName) || isEmpty(document.get(fieldName))) {
                return Optional.of(
                        "MISSING_MANDATORY_FIELD: " + fieldName + " (" + mapping.getAemField() + ")");
            }
        }

        return Optional.empty();
    }

    private static boolean isEmpty(final Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isBlank();
        }
        if (value instanceof List) {
            return ((List<?>) value).isEmpty();
        }
        return false;
    }
}
