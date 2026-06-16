package com.searchstax.aem.connector.core.utils;

/**
 * Helpers for secret fields that are stored in the repository but never returned to the author UI.
 */
public final class SecretFieldSupport {

    private SecretFieldSupport() {
    }

    public static boolean isConfigured(final String value) {
        return value != null && !value.trim().isEmpty();
    }
}
