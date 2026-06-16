package com.searchstax.aem.connector.core.config.wizard;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class SearchStaxWizardApiOsgiConfigurationTest {

    private final AemContext context = new AemContext();

    @Mock
    private ConfigurationAdmin configurationAdmin;

    @Mock
    private Configuration apiConfiguration;

    private SearchStaxWizardOsgiPersistServlet servlet;
    private SearchStaxWizardResourceProvider resourceProvider;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new SearchStaxWizardOsgiPersistServlet();
        injectField(servlet, "configurationAdmin", configurationAdmin);

        resourceProvider = new SearchStaxWizardResourceProvider();
        injectField(resourceProvider, "configurationAdmin", configurationAdmin);
    }

    @Test
    void persistApiConfiguration_usesSingletonPidAndPersistsAnalytics() throws Exception {
        final Hashtable<String, Object> existing = new Hashtable<>();
        existing.put("apiToken", "stored-token");
        existing.put("analyticsTrackingKey", "stored-analytics-key");
        when(configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID))
                .thenReturn(apiConfiguration);
        when(apiConfiguration.getProperties()).thenReturn(existing);

        context.request().setServletPath(SearchStaxWizardBindingPaths.SERVLET_API_SAVE);
        context.request().setMethod("POST");
        context.request().addRequestParameter("endpointUrl", "https://api.example.com");
        context.request().addRequestParameter("selectEndpoint", "https://select.example.com");
        context.request().addRequestParameter("updateEndpoint", "https://update.example.com");
        context.request().addRequestParameter("analyticsTrackingUrl", "https://analytics.example.com/track");
        context.request().addRequestParameter("analyticsReportingUrl", "https://analytics.example.com/report");

        servlet.doPost(context.request(), context.response());

        verify(configurationAdmin, atLeastOnce())
                .getConfiguration(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID);
        verify(configurationAdmin, never())
                .getConfiguration(SearchStaxOsgiConfigurationPids.ENVIRONMENT_CONFIGURATION_PID);

        @SuppressWarnings("rawtypes")
        final ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        verify(apiConfiguration).update(captor.capture());
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> updated = captor.getValue();
        assertEquals("https://api.example.com", updated.get("endpointUrl"));
        assertEquals("stored-token", updated.get("apiToken"));
        assertEquals("https://analytics.example.com/track", updated.get("analyticsTrackingUrl"));
        assertEquals("stored-analytics-key", updated.get("analyticsTrackingKey"));
        assertEquals("https://analytics.example.com/report", updated.get("analyticsReportingUrl"));
        assertNull(updated.get("activeEnvironment"));
    }

    @Test
    void buildApiValueMap_readsSingletonPid() throws Exception {
        final Hashtable<String, Object> props = new Hashtable<>();
        props.put("endpointUrl", "https://read.example.com");
        props.put("discoveryApiKey", "discovery-key");
        props.put("analyticsReportingApiKey", "reporting-key");
        when(configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID))
                .thenReturn(apiConfiguration);
        when(apiConfiguration.getProperties()).thenReturn(props);

        final ValueMap valueMap = invokeBuildApiValueMap(null);

        assertEquals("https://read.example.com", valueMap.get("endpointUrl", String.class));
        assertEquals("discovery-key", valueMap.get("discoveryApiKey", String.class));
        assertEquals("reporting-key", valueMap.get("analyticsReportingApiKey", String.class));
        assertFalse(valueMap.containsKey("activeEnvironment"));

        verify(configurationAdmin).getConfiguration(eq(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID));
        verify(configurationAdmin, never()).getConfiguration(SearchStaxOsgiConfigurationPids.ENVIRONMENT_CONFIGURATION_PID);
    }

    private ValueMap invokeBuildApiValueMap(final ResourceResolver resolver) throws Exception {
        final Method method =
                SearchStaxWizardResourceProvider.class.getDeclaredMethod("buildApiValueMap", ResourceResolver.class);
        method.setAccessible(true);
        return (ValueMap) method.invoke(resourceProvider, resolver);
    }

    private static void injectField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
