package com.searchstax.aem.connector.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretFieldSupportTest {

    @Test
    void isConfigured_returnsFalseForBlankValues() {
        assertFalse(SecretFieldSupport.isConfigured(null));
        assertFalse(SecretFieldSupport.isConfigured(""));
        assertFalse(SecretFieldSupport.isConfigured("   "));
    }

    @Test
    void isConfigured_returnsTrueForNonBlankValue() {
        assertTrue(SecretFieldSupport.isConfigured("secret"));
    }
}
