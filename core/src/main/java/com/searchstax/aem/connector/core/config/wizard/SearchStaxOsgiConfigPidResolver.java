package com.searchstax.aem.connector.core.config.wizard;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves OSGi Configuration Admin PIDs for environment-specific named configurations.
 */
public final class SearchStaxOsgiConfigPidResolver {

    private static final Set<String> ALLOWED =
            Collections.unmodifiableSet(Set.of("dev", "prod", "local"));

    private SearchStaxOsgiConfigPidResolver() {
    }

    /**
     * @param value raw posted or stored value (may be null)
     * @return one of {@code dev}, {@code prod}, {@code local}; never null
     */
    public static String normalizeEnvironment(final String value) {
        if (value == null) {
            return "dev";
        }
        final String v = value.trim().toLowerCase(Locale.ROOT);
        return ALLOWED.contains(v) ? v : "dev";
    }

    /**
     * Named configuration PID: {@code basePid + "~" + environment}.
     */
    public static String namedConfigurationPid(final String basePid, final String environment) {
        return basePid + "~" + normalizeEnvironment(environment);
    }
}
