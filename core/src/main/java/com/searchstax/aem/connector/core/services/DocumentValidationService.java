package com.searchstax.aem.connector.core.services;

import java.util.Map;
import java.util.Optional;

/**
 * Validates built Solr documents against configured mandatory metadata mappings (SRS 5.2.2).
 */
public interface DocumentValidationService {

    /**
     * @return empty if valid, or a skip reason if mandatory fields are missing
     */
    Optional<String> validateMandatoryFields(Map<String, Object> document, String language);
}
