package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.event.jobs.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FullIndexPathConfigTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Job job;

    @Test
    void fromRequest_buildsConfigFromParameters() {
        when(request.getParameter("rootPath")).thenReturn("/content/site");
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("includePaths")))
                .thenReturn(new String[]{"/content/site/en"});
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("./includePaths")))
                .thenReturn(new String[0]);
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("excludePaths")))
                .thenReturn(new String[]{"/content/site/private"});
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("./excludePaths")))
                .thenReturn(new String[0]);
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("includeChildPaths")))
                .thenReturn(new String[]{"true"});
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());

        final FullIndexPathConfig config = FullIndexPathConfig.fromRequest(request);

        assertEquals("/content/site", config.getRootPath());
        assertArrayEquals(new String[]{"/content/site/en"}, config.getIncludePaths());
        assertArrayEquals(new String[]{"/content/site/private"}, config.getExcludePaths());
        assertTrue(config.getIncludeChildPaths()[0]);
    }

    @Test
    void fromJobProperties_roundTripsThroughToJobProperties() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, new String[]{"/content/en"});
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS, new String[]{"true"});
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS, new String[]{"/content/exclude"});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);
        final Map<String, Object> roundTrip = config.toJobProperties();

        assertEquals("/content", roundTrip.get(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH));
        assertArrayEquals(new String[]{"/content/en"}, (String[]) roundTrip.get(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS));
        assertArrayEquals(new String[]{"/content/exclude"}, (String[]) roundTrip.get(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS));
    }

    @Test
    void fromJob_returnsEmptyConfigWhenJobIsNull() {
        final FullIndexPathConfig config = FullIndexPathConfig.fromJob(null);

        assertEquals("", config.getRootPath());
        assertEquals(0, config.getIncludePaths().length);
        assertEquals(0, config.getExcludePaths().length);
    }

    @Test
    void fromJob_readsPropertiesFromJob() {
        final Set<String> propertyNames = new LinkedHashSet<>();
        propertyNames.add(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH);
        propertyNames.add(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS);
        propertyNames.add(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS);
        propertyNames.add(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS);
        when(job.getPropertyNames()).thenReturn(propertyNames);
        when(job.getProperty(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH)).thenReturn("/content/wknd");
        when(job.getProperty(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS))
                .thenReturn(new String[]{"/content/wknd/en"});
        when(job.getProperty(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS)).thenReturn(new String[0]);
        when(job.getProperty(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS))
                .thenReturn(new String[]{"false"});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJob(job);

        assertEquals("/content/wknd", config.getRootPath());
        assertArrayEquals(new String[]{"/content/wknd/en"}, config.getIncludePaths());
        assertFalse(config.getIncludeChildPaths()[0]);
    }

    @Test
    void fromRequest_collectsMultifieldItemParameters() {
        when(request.getParameter("rootPath")).thenReturn("/content/site");
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("includePaths")))
                .thenReturn(new String[0]);
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("./includePaths")))
                .thenReturn(new String[0]);
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("excludePaths")))
                .thenReturn(new String[0]);
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("./excludePaths")))
                .thenReturn(new String[0]);
        when(request.getParameterValues(org.mockito.ArgumentMatchers.eq("includeChildPaths")))
                .thenReturn(new String[]{"true"});
        when(request.getParameterNames())
                .thenReturn(java.util.Collections.enumeration(
                        java.util.List.of("includePaths/item0/path", "includePaths/item1/path")));
        when(request.getParameter("includePaths/item0/path")).thenReturn("/content/site/en");
        when(request.getParameter("includePaths/item1/path")).thenReturn("/content/site/fr");

        final FullIndexPathConfig config = FullIndexPathConfig.fromRequest(request);

        assertArrayEquals(
                new String[]{"/content/site/en", "/content/site/fr"},
                config.getIncludePaths());
    }

    @Test
    void fromJobProperties_handlesSingleStringAndObjectArrayValues() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, "/content/en");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS, new Object[]{" /content/private ", null, ""});
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS, new String[]{"true"});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertEquals("/content", config.getRootPath());
        assertArrayEquals(new String[]{"/content/en"}, config.getIncludePaths());
        assertArrayEquals(new String[]{"/content/private"}, config.getExcludePaths());
        assertTrue(config.getIncludeChildPaths()[0]);
    }

    @Test
    void fromJobProperties_returnsEmptyConfigForNullOrEmptyMap() {
        final FullIndexPathConfig empty = FullIndexPathConfig.fromJobProperties(null);
        assertEquals("", empty.getRootPath());
        assertEquals(0, empty.getIncludePaths().length);

        final FullIndexPathConfig blank = FullIndexPathConfig.fromJobProperties(new HashMap<>());
        assertEquals("", blank.getRootPath());
    }

    @Test
    void gettersReturnDefensiveCopies() {
        final FullIndexPathConfig config = new FullIndexPathConfig(
                "/content",
                new String[]{"/content/a"},
                new boolean[]{true},
                new String[]{"/content/x"});

        config.getIncludePaths()[0] = "/mutated";
        config.getExcludePaths()[0] = "/mutated";
        config.getIncludeChildPaths()[0] = false;

        assertArrayEquals(new String[]{"/content/a"}, config.getIncludePaths());
        assertArrayEquals(new String[]{"/content/x"}, config.getExcludePaths());
        assertTrue(config.getIncludeChildPaths()[0]);
    }
}
