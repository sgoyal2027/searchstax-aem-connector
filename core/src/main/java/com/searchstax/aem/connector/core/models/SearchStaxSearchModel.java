package com.searchstax.aem.connector.core.models;

import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the SearchStax search component (SRS 5.10 UX Toolkit integration).
 */
@Model(
        adaptables = {Resource.class, SlingHttpServletRequest.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SearchStaxSearchModel {

    @ValueMapValue
    private String searchProfile;

    @ValueMapValue
    private String resultsPerPage;

    @ValueMapValue
    private String placeholderText;

    @OSGiService
    private SearchStaxConfigurationService configurationService;

    public String getSelectEndpoint() {
        return configurationService != null ? configurationService.getSelectEndpoint() : "";
    }

    public String getSelectToken() {
        return configurationService != null ? configurationService.getSelectToken() : "";
    }

    public String getSearchProfile() {
        return searchProfile != null ? searchProfile : "";
    }

    public String getResultsPerPage() {
        return resultsPerPage != null && !resultsPerPage.isBlank() ? resultsPerPage : "10";
    }

    public String getPlaceholderText() {
        return placeholderText != null && !placeholderText.isBlank() ? placeholderText : "Search";
    }
}
