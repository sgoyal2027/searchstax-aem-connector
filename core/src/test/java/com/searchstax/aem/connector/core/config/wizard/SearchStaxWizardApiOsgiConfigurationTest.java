package com.searchstax.aem.connector.core.config.wizard;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
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

    @Mock
    private Configuration environmentConfiguration;

    @Mock
    private Configuration fullIndexConfiguration;

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

    @Test
    void persistFullIndexConfiguration_updatesEnvironmentAndNamedPid() throws Exception {
        final Hashtable<String, Object> envProps = new Hashtable<>();
        envProps.put("activeEnvironment", "dev");
        when(configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.ENVIRONMENT_CONFIGURATION_PID))
                .thenReturn(environmentConfiguration);
        when(environmentConfiguration.getProperties()).thenReturn(envProps);

        final String fullIndexPid =
                SearchStaxOsgiConfigPidResolver.namedConfigurationPid(
                        SearchStaxOsgiConfigurationPids.FULL_INDEX_CONFIGURATION_PID, "prod");
        when(configurationAdmin.getConfiguration(fullIndexPid)).thenReturn(fullIndexConfiguration);
        when(fullIndexConfiguration.getProperties()).thenReturn(new Hashtable<>());

        context.request().setServletPath(SearchStaxWizardBindingPaths.SERVLET_FULL_INDEX_SAVE);
        context.request().setMethod("POST");
        context.request().addRequestParameter("activeEnvironment", "prod");
        context.request().addRequestParameter("includePaths", "/content/wknd");
        context.request().addRequestParameter("excludePaths", "/content/wknd/private");

        servlet.doPost(context.request(), context.response());

        @SuppressWarnings("rawtypes")
        final ArgumentCaptor<Dictionary> envCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(environmentConfiguration).update(envCaptor.capture());
        assertEquals("prod", envCaptor.getValue().get("activeEnvironment"));

        @SuppressWarnings("rawtypes")
        final ArgumentCaptor<Dictionary> fullIndexCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(fullIndexConfiguration).update(fullIndexCaptor.capture());
        assertEquals(
                List.of("/content/wknd"),
                Arrays.asList((String[]) fullIndexCaptor.getValue().get("includePaths")));
        assertEquals(
                List.of("/content/wknd/private"),
                Arrays.asList((String[]) fullIndexCaptor.getValue().get("excludePaths")));
    }

    @Test
    void getResource_apiJcrContent_exposesOsgiValues() throws Exception {
        final Hashtable<String, Object> props = new Hashtable<>();
        props.put("endpointUrl", "https://resource.example.com");
        when(configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID))
                .thenReturn(apiConfiguration);
        when(apiConfiguration.getProperties()).thenReturn(props);

        final ResolveContext<Void> resolveContext = mock(ResolveContext.class);
        when(resolveContext.getResourceResolver()).thenReturn(context.resourceResolver());

        final Resource resource = resourceProvider.getResource(
                resolveContext,
                SearchStaxWizardBindingPaths.API_JCR_CONTENT,
                mock(ResourceContext.class),
                null);

        assertNotNull(resource);
        final ValueMap valueMap = resource.adaptTo(ValueMap.class);
        assertEquals("https://resource.example.com", valueMap.get("endpointUrl", String.class));
    }

    @Test
    void listChildren_root_returnsApiAndFullIndexPages() {
        final ResolveContext<Void> resolveContext = mock(ResolveContext.class);
        when(resolveContext.getResourceResolver()).thenReturn(context.resourceResolver());

        final Resource root = resourceProvider.getResource(
                resolveContext,
                SearchStaxWizardBindingPaths.ROOT,
                mock(ResourceContext.class),
                null);
        final Iterator<Resource> children = resourceProvider.listChildren(resolveContext, root);

        assertNotNull(children);
        final List<String> childPaths = Arrays.asList(
                children.next().getPath(),
                children.next().getPath());
        assertTrue(childPaths.contains(SearchStaxWizardBindingPaths.API_PAGE));
        assertTrue(childPaths.contains(SearchStaxWizardBindingPaths.FULL_INDEX_PAGE));
    }

    @Test
    void buildApiValueMap_mergesLegacyConfWhenOsgiEmpty() throws Exception {
        context.create().resource(
                SearchStaxLegacyWizardConfPaths.API_JCR_CONTENT,
                "endpointUrl",
                "https://legacy.example.com",
                "discoveryApiKey",
                "legacy-discovery");

        when(configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID))
                .thenReturn(apiConfiguration);
        when(apiConfiguration.getProperties()).thenReturn(new Hashtable<>());

        final ValueMap valueMap = invokeBuildApiValueMap(context.resourceResolver());

        assertEquals("https://legacy.example.com", valueMap.get("endpointUrl", String.class));
        assertEquals("legacy-discovery", valueMap.get("discoveryApiKey", String.class));
    }

    @Test
    void buildFullIndexValueMap_readsEnvironmentSpecificPid() throws Exception {
        final Hashtable<String, Object> envProps = new Hashtable<>();
        envProps.put("activeEnvironment", "prod");
        when(configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.ENVIRONMENT_CONFIGURATION_PID))
                .thenReturn(environmentConfiguration);
        when(environmentConfiguration.getProperties()).thenReturn(envProps);

        final String fullIndexPid =
                SearchStaxOsgiConfigPidResolver.namedConfigurationPid(
                        SearchStaxOsgiConfigurationPids.FULL_INDEX_CONFIGURATION_PID, "prod");
        final Hashtable<String, Object> fullIndexProps = new Hashtable<>();
        fullIndexProps.put("includePaths", new String[]{"/content/prod"});
        when(configurationAdmin.getConfiguration(fullIndexPid)).thenReturn(fullIndexConfiguration);
        when(fullIndexConfiguration.getProperties()).thenReturn(fullIndexProps);

        final ValueMap valueMap = invokeBuildFullIndexValueMap(null);

        assertEquals("prod", valueMap.get("activeEnvironment", String.class));
        assertEquals("/content/prod", valueMap.get("includePaths", String[].class)[0]);
    }

    private ValueMap invokeBuildFullIndexValueMap(final ResourceResolver resolver) throws Exception {
        final Method method =
                SearchStaxWizardResourceProvider.class.getDeclaredMethod(
                        "buildFullIndexValueMap", ResourceResolver.class);
        method.setAccessible(true);
        return (ValueMap) method.invoke(resourceProvider, resolver);
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
