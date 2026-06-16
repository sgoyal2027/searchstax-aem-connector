package com.searchstax.aem.connector.core.config.wizard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchStaxOsgiConfigPidResolverTest {

    @Test
    void normalizeEnvironment_acceptsAllowed() {
        assertEquals("dev", SearchStaxOsgiConfigPidResolver.normalizeEnvironment("dev"));
        assertEquals("prod", SearchStaxOsgiConfigPidResolver.normalizeEnvironment("PROD"));
        assertEquals("local", SearchStaxOsgiConfigPidResolver.normalizeEnvironment(" Local "));
    }

    @Test
    void normalizeEnvironment_defaultsForUnknown() {
        assertEquals("dev", SearchStaxOsgiConfigPidResolver.normalizeEnvironment(null));
        assertEquals("dev", SearchStaxOsgiConfigPidResolver.normalizeEnvironment(""));
        assertEquals("dev", SearchStaxOsgiConfigPidResolver.normalizeEnvironment("staging"));
    }

    @Test
    void namedConfigurationPid_appendsTildeEnv() {
        assertEquals(
                "com.foo.Bar~prod",
                SearchStaxOsgiConfigPidResolver.namedConfigurationPid("com.foo.Bar", "prod"));
    }
}
