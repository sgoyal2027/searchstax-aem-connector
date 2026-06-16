package com.searchstax.aem.connector.core.config.wizard;

import com.day.cq.wcm.api.NameConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes wizard resource paths under {@code /apps/searchstaxconnector/wizard-bindings} backed by OSGi configuration
 * so Granite forms continue to resolve {@code jcr:content} without reading {@code /conf}.
 */
@Component(
        service = ResourceProvider.class,
        property = {
                ResourceProvider.PROPERTY_ROOT + "=" + SearchStaxWizardBindingPaths.ROOT
        }
)
public class SearchStaxWizardResourceProvider extends ResourceProvider<Void> {

    private static final String RT_PAGE = NameConstants.NT_PAGE;
    private static final String RT_PAGE_CONTENT = "cq:PageContent";

    private static final String[] API_LEGACY_KEYS = {
            "endpointUrl",
            "apiToken",
            "selectEndpoint",
            "selectToken",
            "updateEndpoint",
            "updateToken",
            "autoSuggestApi",
            "relatedSearchesEndpoint",
            "popularSearchesEndpoint",
            "discoveryApiKey",
            "analyticsTrackingUrl",
            "analyticsTrackingKey",
            "analyticsReportingUrl",
            "analyticsReportingApiKey",
            "forwardGeocodingEndpoint",
            "reverseGeocodingEndpoint"
    };

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @Override
    public Resource getResource(
            final ResolveContext<Void> ctx,
            final String path,
            final ResourceContext resourceContext,
            final Resource parent) {
        final ResourceResolver resolver = ctx.getResourceResolver();
        if (SearchStaxWizardBindingPaths.ROOT.equals(path)) {
            return new OsgiBackedValueMapResource(resolver, path, RT_PAGE, emptyVm());
        }
        if (SearchStaxWizardBindingPaths.API_PAGE.equals(path)) {
            return new OsgiBackedValueMapResource(resolver, path, RT_PAGE, emptyVm());
        }
        if (SearchStaxWizardBindingPaths.FULL_INDEX_PAGE.equals(path)) {
            return new OsgiBackedValueMapResource(resolver, path, RT_PAGE, emptyVm());
        }
        if (SearchStaxWizardBindingPaths.API_JCR_CONTENT.equals(path)) {
            return new OsgiBackedValueMapResource(resolver, path, RT_PAGE_CONTENT, buildApiValueMap(resolver));
        }
        if (SearchStaxWizardBindingPaths.FULL_INDEX_JCR_CONTENT.equals(path)) {
            return new OsgiBackedValueMapResource(resolver, path, RT_PAGE_CONTENT, buildFullIndexValueMap(resolver));
        }
        return null;
    }

    @Override
    public Iterator<Resource> listChildren(final ResolveContext<Void> ctx, final Resource parent) {
        final ResourceResolver resolver = ctx.getResourceResolver();
        final String path = parent.getPath();
        if (SearchStaxWizardBindingPaths.ROOT.equals(path)) {
            return Arrays.<Resource>asList(
                            new OsgiBackedValueMapResource(resolver, SearchStaxWizardBindingPaths.API_PAGE, RT_PAGE, emptyVm()),
                            new OsgiBackedValueMapResource(resolver, SearchStaxWizardBindingPaths.FULL_INDEX_PAGE, RT_PAGE, emptyVm()))
                    .iterator();
        }
        if (SearchStaxWizardBindingPaths.API_PAGE.equals(path) || SearchStaxWizardBindingPaths.FULL_INDEX_PAGE.equals(path)) {
            final String jcrPath = path + "/jcr:content";
            final ValueMap vm =
                    SearchStaxWizardBindingPaths.API_PAGE.equals(path)
                            ? buildApiValueMap(resolver)
                            : buildFullIndexValueMap(resolver);
            return Collections.<Resource>singletonList(new OsgiBackedValueMapResource(resolver, jcrPath, RT_PAGE_CONTENT, vm))
                    .iterator();
        }
        return null;
    }

    private ValueMap buildApiValueMap(final ResourceResolver resolver) {
        final Dictionary<String, Object> props =
                readConfiguration(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID);
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("endpointUrl", getString(props, "endpointUrl"));
        map.put("apiToken", getString(props, "apiToken"));
        map.put("selectEndpoint", getString(props, "selectEndpoint"));
        map.put("selectToken", getString(props, "selectToken"));
        map.put("updateEndpoint", getString(props, "updateEndpoint"));
        map.put("updateToken", getString(props, "updateToken"));
        map.put("autoSuggestApi", getString(props, "autoSuggestApi"));
        map.put("relatedSearchesEndpoint", getString(props, "relatedSearchesEndpoint"));
        map.put("popularSearchesEndpoint", getString(props, "popularSearchesEndpoint"));
        map.put("discoveryApiKey", getString(props, "discoveryApiKey"));
        map.put("analyticsTrackingUrl", getString(props, "analyticsTrackingUrl"));
        map.put("analyticsTrackingKey", getString(props, "analyticsTrackingKey"));
        map.put("analyticsReportingUrl", getString(props, "analyticsReportingUrl"));
        map.put("analyticsReportingApiKey", getString(props, "analyticsReportingApiKey"));
        map.put("forwardGeocodingEndpoint", getString(props, "forwardGeocodingEndpoint"));
        map.put("reverseGeocodingEndpoint", getString(props, "reverseGeocodingEndpoint"));
        mergeLegacyApiIfNeeded(resolver, map, props);
        return new ValueMapDecorator(map);
    }

    private ValueMap buildFullIndexValueMap(final ResourceResolver resolver) {
        final String activeEnv = readActiveEnvironment();
        final String pid =
                SearchStaxOsgiConfigPidResolver.namedConfigurationPid(
                        SearchStaxOsgiConfigurationPids.FULL_INDEX_CONFIGURATION_PID, activeEnv);
        final Dictionary<String, Object> props = readConfiguration(pid);
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("activeEnvironment", activeEnv);
        map.put("includePaths", getStringArray(props, "includePaths"));
        map.put("excludePaths", getStringArray(props, "excludePaths"));
        mergeLegacyFullIndexIfNeeded(resolver, map, props);
        return new ValueMapDecorator(map);
    }

    private String readActiveEnvironment() {
        final Dictionary<String, Object> envProps =
                readConfiguration(SearchStaxOsgiConfigurationPids.ENVIRONMENT_CONFIGURATION_PID);
        return SearchStaxOsgiConfigPidResolver.normalizeEnvironment(getString(envProps, "activeEnvironment"));
    }

    private void mergeLegacyApiIfNeeded(
            final ResourceResolver resolver,
            final Map<String, Object> map,
            final Dictionary<String, Object> props) {
        if (resolver == null || !isApiFromOsgiEmpty(props, map)) {
            return;
        }
        final Resource legacy = resolver.getResource(SearchStaxLegacyWizardConfPaths.API_JCR_CONTENT);
        if (legacy == null) {
            return;
        }
        final ValueMap vm = legacy.adaptTo(ValueMap.class);
        if (vm == null) {
            return;
        }
        for (final String key : API_LEGACY_KEYS) {
            if (isEmptyDisplayValue(map.get(key)) && vm.get(key) != null) {
                map.put(key, vm.get(key));
            }
        }
    }

    private static boolean isApiFromOsgiEmpty(final Dictionary<String, Object> props, final Map<String, Object> map) {
        if (props == null) {
            return true;
        }
        final boolean endpointsEmpty =
                getString(props, "endpointUrl").isEmpty()
                        && getString(props, "selectEndpoint").isEmpty()
                        && getString(props, "updateEndpoint").isEmpty();
        return endpointsEmpty && isEmptyDisplayValue(map.get("endpointUrl"));
    }

    private void mergeLegacyFullIndexIfNeeded(
            final ResourceResolver resolver,
            final Map<String, Object> map,
            final Dictionary<String, Object> props) {
        if (resolver == null || !isFullIndexFromOsgiEmpty(props, map)) {
            return;
        }
        final Resource legacy = resolver.getResource(SearchStaxLegacyWizardConfPaths.FULL_INDEX_JCR_CONTENT);
        if (legacy == null) {
            return;
        }
        final ValueMap vm = legacy.adaptTo(ValueMap.class);
        if (vm == null) {
            return;
        }
        if (isEmptyStringArray(map.get("includePaths")) && vm.get("includePaths") != null) {
            map.put("includePaths", toStringArrayProperty(vm.get("includePaths")));
        }
        if (isEmptyStringArray(map.get("excludePaths")) && vm.get("excludePaths") != null) {
            map.put("excludePaths", toStringArrayProperty(vm.get("excludePaths")));
        }
    }

    private static String[] toStringArrayProperty(final Object raw) {
        if (raw == null) {
            return new String[0];
        }
        if (raw instanceof String[]) {
            return (String[]) raw;
        }
        if (raw instanceof String) {
            final String s = ((String) raw).trim();
            return s.isEmpty() ? new String[0] : new String[] {s};
        }
        if (raw instanceof Collection) {
            final List<String> list = new ArrayList<>();
            for (final Object item : (Collection<?>) raw) {
                if (item != null && !item.toString().trim().isEmpty()) {
                    list.add(item.toString().trim());
                }
            }
            return list.toArray(new String[0]);
        }
        if (raw.getClass().isArray()) {
            final int len = java.lang.reflect.Array.getLength(raw);
            final List<String> list = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                final Object item = java.lang.reflect.Array.get(raw, i);
                if (item != null && !item.toString().trim().isEmpty()) {
                    list.add(item.toString().trim());
                }
            }
            return list.toArray(new String[0]);
        }
        return new String[0];
    }

    private static boolean isFullIndexFromOsgiEmpty(final Dictionary<String, Object> props, final Map<String, Object> map) {
        final String[] inc = getStringArray(props, "includePaths");
        final String[] exc = getStringArray(props, "excludePaths");
        if ((inc.length > 0 || exc.length > 0)) {
            return false;
        }
        return isEmptyStringArray(map.get("includePaths")) && isEmptyStringArray(map.get("excludePaths"));
    }

    private static boolean isEmptyDisplayValue(final Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Boolean) {
            return false;
        }
        if (value instanceof String[]) {
            return ((String[]) value).length == 0;
        }
        return false;
    }

    private static boolean isEmptyStringArray(final Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String[]) {
            return ((String[]) value).length == 0;
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        return true;
    }

    private Dictionary<String, Object> readConfiguration(final String pid) {
        try {
            final Configuration configuration = configurationAdmin.getConfiguration(pid);
            return configuration.getProperties();
        } catch (final IOException | RuntimeException e) {
            return null;
        }
    }

    private static String getString(final Dictionary<String, Object> props, final String key) {
        if (props == null) {
            return "";
        }
        final Object value = props.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String[] getStringArray(final Dictionary<String, Object> props, final String key) {
        if (props == null) {
            return new String[0];
        }
        final Object value = props.get(key);
        if (value == null) {
            return new String[0];
        }
        if (value instanceof String[]) {
            return (String[]) value;
        }
        if (value instanceof String) {
            final String s = (String) value;
            return s.isEmpty() ? new String[0] : new String[] {s};
        }
        if (value instanceof Collection) {
            final List<String> list = new ArrayList<>();
            for (final Object item : (Collection<?>) value) {
                if (item != null && !item.toString().trim().isEmpty()) {
                    list.add(item.toString().trim());
                }
            }
            return list.toArray(new String[0]);
        }
        return new String[0];
    }

    private static ValueMap emptyVm() {
        return new ValueMapDecorator(Collections.emptyMap());
    }
}
