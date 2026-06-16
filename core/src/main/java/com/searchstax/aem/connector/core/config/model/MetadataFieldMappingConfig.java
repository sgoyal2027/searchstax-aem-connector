package com.searchstax.aem.connector.core.config.model;

public class MetadataFieldMappingConfig {

    private String aemField;
    private String customProperty;
    private String searchStaxField;
    private String searchStaxFieldType;
    private boolean enabled;
    private boolean mandatory;

    public String getAemField() {
        return aemField;
    }

    public void setAemField(String aemField) {
        this.aemField = aemField;
    }

    public String getCustomProperty() {
        return customProperty;
    }

    public void setCustomProperty(String customProperty) {
        this.customProperty = customProperty;
    }

    public String getSearchStaxField() {
        return searchStaxField;
    }

    public void setSearchStaxField(String searchStaxField) {
        this.searchStaxField = searchStaxField;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSearchStaxFieldType() {
        return searchStaxFieldType;
    }

    public void setSearchStaxFieldType(String searchStaxFieldType) {
        this.searchStaxFieldType = searchStaxFieldType;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
}
