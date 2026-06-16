package com.searchstax.aem.connector.core.config.wizard;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persists Granite wizard submissions to OSGi Configuration Admin (instead of {@code /conf}).
 */
@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=SearchStax wizard OSGi configuration persistence",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SearchStaxWizardBindingPaths.SERVLET_API_SAVE,
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SearchStaxWizardBindingPaths.SERVLET_FULL_INDEX_SAVE,
                Constants.SERVICE_RANKING + ":Integer=200000"
        }
)
public class SearchStaxWizardOsgiPersistServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    private transient ConfigurationAdmin configurationAdmin;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (matchesSaveServlet(request, SearchStaxWizardBindingPaths.SERVLET_API_SAVE)) {
                persistApiConfiguration(request);
            } else if (matchesSaveServlet(request, SearchStaxWizardBindingPaths.SERVLET_FULL_INDEX_SAVE)) {
                persistFullIndexConfiguration(request);
            } else {
                response.sendError(SlingHttpServletResponse.SC_NOT_FOUND);
                return;
            }
            sendRedirectBack(request, response);
        } catch (final IOException ex) {
            throw ex;
        } catch (final RuntimeException ex) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private void persistApiConfiguration(final SlingHttpServletRequest request) throws IOException {
        final Configuration configuration =
                configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.API_CONFIGURATION_PID);
        final Dictionary<String, Object> existing = getProperties(configuration);
        final Dictionary<String, Object> next = baseDictionary(existing);
        next.put("endpointUrl", trimToEmpty(request.getParameter("endpointUrl")));
        putPassword(next, existing, "apiToken", request.getParameter("apiToken"));
        next.put("selectEndpoint", trimToEmpty(request.getParameter("selectEndpoint")));
        putPassword(next, existing, "selectToken", request.getParameter("selectToken"));
        next.put("updateEndpoint", trimToEmpty(request.getParameter("updateEndpoint")));
        putPassword(next, existing, "updateToken", request.getParameter("updateToken"));
        next.put("autoSuggestApi", trimToEmpty(request.getParameter("autoSuggestApi")));
        next.put("relatedSearchesEndpoint", trimToEmpty(request.getParameter("relatedSearchesEndpoint")));
        next.put("popularSearchesEndpoint", trimToEmpty(request.getParameter("popularSearchesEndpoint")));
        putPassword(next, existing, "discoveryApiKey", request.getParameter("discoveryApiKey"));
        next.put("analyticsTrackingUrl", trimToEmpty(request.getParameter("analyticsTrackingUrl")));
        putPassword(next, existing, "analyticsTrackingKey", request.getParameter("analyticsTrackingKey"));
        next.put("analyticsReportingUrl", trimToEmpty(request.getParameter("analyticsReportingUrl")));
        putPassword(next, existing, "analyticsReportingApiKey", request.getParameter("analyticsReportingApiKey"));
        next.put("forwardGeocodingEndpoint", trimToEmpty(request.getParameter("forwardGeocodingEndpoint")));
        next.put("reverseGeocodingEndpoint", trimToEmpty(request.getParameter("reverseGeocodingEndpoint")));
        configuration.update(next);
    }

    private void persistFullIndexConfiguration(final SlingHttpServletRequest request) throws IOException {
        final String targetEnv = resolveTargetEnvironment(request);
        persistActiveEnvironmentSingleton(targetEnv);
        final String pid =
                SearchStaxOsgiConfigPidResolver.namedConfigurationPid(
                        SearchStaxOsgiConfigurationPids.FULL_INDEX_CONFIGURATION_PID, targetEnv);
        final Configuration configuration = configurationAdmin.getConfiguration(pid);
        final Dictionary<String, Object> existing = getProperties(configuration);
        final Dictionary<String, Object> next = baseDictionary(existing);
        next.put("includePaths", readPathArray(request, "includePaths"));
        next.put("excludePaths", readPathArray(request, "excludePaths"));
        configuration.update(next);
    }

    private String resolveTargetEnvironment(final SlingHttpServletRequest request) {
        final String p1 = trimToEmpty(request.getParameter("activeEnvironment"));
        final String p2 = trimToEmpty(request.getParameter("./activeEnvironment"));
        final String posted = !p1.isEmpty() ? p1 : p2;
        if (!posted.isEmpty()) {
            return SearchStaxOsgiConfigPidResolver.normalizeEnvironment(posted);
        }
        return readActiveEnvironmentFromSingleton();
    }

    private void persistActiveEnvironmentSingleton(final String targetEnv) throws IOException {
        final Configuration configuration =
                configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.ENVIRONMENT_CONFIGURATION_PID);
        final Dictionary<String, Object> existing = getProperties(configuration);
        final Dictionary<String, Object> next = baseDictionary(existing);
        next.put("activeEnvironment", targetEnv);
        configuration.update(next);
    }

    private String readActiveEnvironmentFromSingleton() {
        try {
            final Configuration configuration =
                    configurationAdmin.getConfiguration(SearchStaxOsgiConfigurationPids.ENVIRONMENT_CONFIGURATION_PID);
            final Dictionary<String, Object> props = configuration.getProperties();
            return SearchStaxOsgiConfigPidResolver.normalizeEnvironment(dictString(props, "activeEnvironment"));
        } catch (final IOException | RuntimeException e) {
            return "dev";
        }
    }

    private static String dictString(final Dictionary<String, Object> props, final String key) {
        if (props == null) {
            return "";
        }
        final Object value = props.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Dictionary<String, Object> getProperties(final Configuration configuration) {
        return configuration.getProperties();
    }

    private static Hashtable<String, Object> baseDictionary(final Dictionary<String, Object> existing) {
        final Hashtable<String, Object> copy = new Hashtable<>();
        if (existing != null) {
            final Enumeration<String> keys = existing.keys();
            while (keys.hasMoreElements()) {
                final String key = keys.nextElement();
                if ("service.pid".equals(key) || "felix.fileinstall.filename".equals(key)) {
                    continue;
                }
                copy.put(key, existing.get(key));
            }
        }
        return copy;
    }

    private void putPassword(
            final Dictionary<String, Object> target,
            final Dictionary<String, Object> existing,
            final String key,
            final String posted) {
        final String trimmed = posted == null ? "" : posted.trim();
        if (!trimmed.isEmpty()) {
            try {
                // Encrypt the plaintext token before storing
                final String protected_value = cryptoSupport.protect(trimmed);
                target.put(key, protected_value);
            } catch (final CryptoException e) {
                // Fallback: store plaintext if encryption fails (shouldn't happen, but fail gracefully)
                target.put(key, trimmed);
            }
        } else if (existing != null && existing.get(key) != null) {
            // Preserve existing encrypted value if no new value provided
            target.put(key, existing.get(key));
        } else {
            target.put(key, "");
        }
    }

    private static String[] readPathArray(final SlingHttpServletRequest request, final String fieldName) {
        final Set<String> ordered = new LinkedHashSet<>();
        addAllParameters(request, fieldName, ordered);
        addAllParameters(request, "./" + fieldName, ordered);
        collectMultifieldPathParameters(request, fieldName, ordered);
        return ordered.toArray(new String[0]);
    }

    private static void addAllParameters(
            final SlingHttpServletRequest request,
            final String name,
            final Set<String> target) {
        final String[] values = request.getParameterValues(name);
        if (values == null) {
            return;
        }
        for (final String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                target.add(value.trim());
            }
        }
    }

    private static void collectMultifieldPathParameters(
            final SlingHttpServletRequest request,
            final String multifieldRoot,
            final Set<String> target) {
        final Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            if (name.contains("@TypeHint")) {
                continue;
            }
            if (!name.contains(multifieldRoot + "/item")) {
                continue;
            }
            final String value = request.getParameter(name);
            if (value != null && !value.trim().isEmpty()) {
                target.add(value.trim());
            }
        }
    }

    private static void sendRedirectBack(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        final String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect("/aem/start.html");
        }
    }

    private static String trimToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Matches POST to our {@code /bin/...} save endpoints (including optional selectors/extension).
     */
    private static boolean matchesSaveServlet(final SlingHttpServletRequest request, final String servletPath) {
        final Resource resource = request.getResource();
        if (resource != null && servletPath.equals(resource.getPath())) {
            return true;
        }
        final String servletPathFromRequest = request.getServletPath();
        if (servletPath.equals(servletPathFromRequest)) {
            return true;
        }
        final String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        final int q = uri.indexOf('?');
        final String pathOnly = q >= 0 ? uri.substring(0, q) : uri;
        return pathOnly.endsWith(servletPath) || pathOnly.contains(servletPath + ".");
    }
}
