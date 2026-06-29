package com.searchstax.aem.connector.core.config.wizard;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxWizardResourceProviderHelperTest {

    @Mock
    private ConfigurationAdmin configurationAdmin;

    private SearchStaxWizardResourceProvider resourceProvider;

    @BeforeEach
    void setUp() throws Exception {
        resourceProvider = new SearchStaxWizardResourceProvider();
        final var field = SearchStaxWizardResourceProvider.class.getDeclaredField("configurationAdmin");
        field.setAccessible(true);
        field.set(resourceProvider, configurationAdmin);
    }

    @Test
    void toStringArrayProperty_coercesCollectionAndPrimitiveArray() throws Exception {
        assertArrayEquals(new String[] {"a", "b"}, invokeToStringArrayProperty(List.of("a", "b")));
        assertArrayEquals(new String[] {"x"}, invokeToStringArrayProperty(new String[] {"x"}));
        assertArrayEquals(new String[] {"solo"}, invokeToStringArrayProperty("solo"));
        assertEquals(0, invokeToStringArrayProperty("  ").length);
        assertEquals(0, invokeToStringArrayProperty(new Object[] {null, "  "}).length);
    }

    @Test
    void isEmptyDisplayValue_handlesStringsBooleansAndArrays() throws Exception {
        assertTrue(invokeBoolean("isEmptyDisplayValue", null));
        assertTrue(invokeBoolean("isEmptyDisplayValue", "  "));
        assertFalse(invokeBoolean("isEmptyDisplayValue", Boolean.TRUE));
        assertTrue(invokeBoolean("isEmptyDisplayValue", new String[0]));
    }

    @Test
    void isEmptyStringArray_handlesCollectionsAndStrings() throws Exception {
        assertTrue(invokeBoolean("isEmptyStringArray", null));
        assertTrue(invokeBoolean("isEmptyStringArray", List.of()));
        assertTrue(invokeBoolean("isEmptyStringArray", " "));
        assertFalse(invokeBoolean("isEmptyStringArray", new String[] {"a"}));
    }

    @Test
    void readConfiguration_returnsNullWhenConfigurationAdminThrows() throws Exception {
        when(configurationAdmin.getConfiguration("test.pid")).thenThrow(new IOException("unavailable"));

        assertNull(invokeReadConfiguration("test.pid"));
    }

    @Test
    void getStringAndArray_handleNullDictionary() throws Exception {
        assertEquals("", invokeGetString(null, "key"));
        assertEquals(0, invokeGetStringArray(null, "key").length);
    }

    @Test
    void listChildren_returnsNullForUnknownParent() {
        final ResolveContext<Void> resolveContext = mock(ResolveContext.class);
        when(resolveContext.getResourceResolver()).thenReturn(mock(ResourceResolver.class));

        final Resource unknownParent = mock(Resource.class);
        when(unknownParent.getPath()).thenReturn(SearchStaxWizardBindingPaths.ROOT + "/unknown");

        final Iterator<Resource> children = resourceProvider.listChildren(resolveContext, unknownParent);

        assertNull(children);
    }

    @Test
    void listChildren_fullIndexPage_returnsJcrContentChild() {
        final ResolveContext<Void> resolveContext = mock(ResolveContext.class);
        final ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolveContext.getResourceResolver()).thenReturn(resolver);

        final Resource fullIndexPage = mock(Resource.class);
        when(fullIndexPage.getPath()).thenReturn(SearchStaxWizardBindingPaths.FULL_INDEX_PAGE);

        final Iterator<Resource> children = resourceProvider.listChildren(resolveContext, fullIndexPage);

        assertNotNull(children);
        assertTrue(children.hasNext());
        assertEquals(SearchStaxWizardBindingPaths.FULL_INDEX_JCR_CONTENT, children.next().getPath());
    }

    @Test
    void getStringArray_readsSingleStringValueFromDictionary() throws Exception {
        final Hashtable<String, Object> props = new Hashtable<>();
        props.put("includePaths", "/content/wknd");

        assertArrayEquals(new String[] {"/content/wknd"}, invokeGetStringArray(props, "includePaths"));
    }

    private static void assertArrayEquals(final String[] expected, final String[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    private String[] invokeToStringArrayProperty(final Object raw) throws Exception {
        final Method method =
                SearchStaxWizardResourceProvider.class.getDeclaredMethod("toStringArrayProperty", Object.class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, raw);
    }

    private boolean invokeBoolean(final String methodName, final Object value) throws Exception {
        final Method method =
                SearchStaxWizardResourceProvider.class.getDeclaredMethod(methodName, Object.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, value);
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, Object> invokeReadConfiguration(final String pid) throws Exception {
        final Method method =
                SearchStaxWizardResourceProvider.class.getDeclaredMethod("readConfiguration", String.class);
        method.setAccessible(true);
        return (Dictionary<String, Object>) method.invoke(resourceProvider, pid);
    }

    private String invokeGetString(final Dictionary<String, Object> props, final String key) throws Exception {
        final Method method =
                SearchStaxWizardResourceProvider.class.getDeclaredMethod("getString", Dictionary.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, props, key);
    }

    private String[] invokeGetStringArray(final Dictionary<String, Object> props, final String key) throws Exception {
        final Method method =
                SearchStaxWizardResourceProvider.class.getDeclaredMethod(
                        "getStringArray", Dictionary.class, String.class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, props, key);
    }
}
