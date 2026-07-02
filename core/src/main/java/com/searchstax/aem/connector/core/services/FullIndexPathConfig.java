package com.searchstax.aem.connector.core.services;



import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;

import org.apache.sling.api.SlingHttpServletRequest;

import org.apache.sling.event.jobs.Job;



import java.util.ArrayList;

import java.util.Arrays;

import java.util.Enumeration;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Map;

import java.util.Set;



/**

 * Path configuration for a full-index run (root paths, include paths with per-path child flags, exclude).

 */

public final class FullIndexPathConfig {



    private final String[] rootPaths;

    private final String[] includePaths;

    private final boolean[] includeChildPaths;

    private final String[] excludePaths;



    public FullIndexPathConfig(

            final String[] rootPaths,

            final String[] includePaths,

            final boolean[] includeChildPaths,

            final String[] excludePaths) {

        this.rootPaths = rootPaths == null ? new String[0] : rootPaths;

        this.includePaths = copy(includePaths);

        this.excludePaths = copy(excludePaths);

        this.includeChildPaths = copyBooleans(includeChildPaths, this.includePaths.length);

    }



    public String[] getRootPaths() {

        return copy(rootPaths);

    }



    public String[] getIncludePaths() {

        return copy(includePaths);

    }



    public String[] getExcludePaths() {

        return copy(excludePaths);

    }



    public boolean[] getIncludeChildPaths() {

        return Arrays.copyOf(includeChildPaths, includeChildPaths.length);

    }



    public static FullIndexPathConfig fromRequest(final SlingHttpServletRequest request) {

        final String[] rootPaths = readPathArray(request, "rootPaths");

        final String[] includes = readPathArray(request, "includePaths");

        final String[] excludes = readPathArray(request, "excludePaths");

        final boolean[] includeChildPaths = readBooleanArray(request, "includeChildPaths", includes.length);

        return new FullIndexPathConfig(rootPaths, includes, includeChildPaths, excludes);

    }



    public static FullIndexPathConfig fromJob(final Job job) {

        if (job == null) {

            return new FullIndexPathConfig(new String[0], new String[0], new boolean[0], new String[0]);

        }

        final Map<String, Object> props = new java.util.LinkedHashMap<>();

        for (final String name : job.getPropertyNames()) {

            props.put(name, job.getProperty(name));

        }

        return fromJobProperties(props);

    }



    public static FullIndexPathConfig fromJobProperties(final Map<String, Object> props) {

        if (props == null || props.isEmpty()) {

            return new FullIndexPathConfig(new String[0], new String[0], new boolean[0], new String[0]);

        }

        final String[] root = stringArrayProperty(props, SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATHS);

        final String[] includes = stringArrayProperty(props, SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS);

        final String[] excludes = stringArrayProperty(props, SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS);

        final boolean[] includeChildPaths = booleanArrayFromStrings(
                stringArrayProperty(props, SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS),
                includes.length);

        return new FullIndexPathConfig(root, includes, includeChildPaths, excludes);

    }



    public Map<String, Object> toJobProperties() {

        final Map<String, Object> props = new java.util.LinkedHashMap<>();

        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATHS, rootPaths);

        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, includePaths);

        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS, booleanArrayToStrings(includeChildPaths));

        props.put(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS, excludePaths);

        return props;

    }



    private static boolean[] copyBooleans(final boolean[] source, final int targetLength) {

        final boolean[] result = new boolean[targetLength];

        Arrays.fill(result, false);

        if (source != null) {

            System.arraycopy(source, 0, result, 0, Math.min(source.length, targetLength));

        }

        return result;

    }



    private static boolean[] readBooleanArray(

            final SlingHttpServletRequest request, final String paramName, final int expectedLength) {

        final String[] values = request.getParameterValues(paramName);

        final boolean[] result = new boolean[expectedLength];

        Arrays.fill(result, false);

        if (values == null) {

            return result;

        }

        for (int i = 0; i < Math.min(values.length, expectedLength); i++) {

            result[i] = Boolean.parseBoolean(values[i] != null ? values[i].trim() : "");

        }

        return result;

    }



    private static boolean[] booleanArrayFromStrings(final String[] values, final int expectedLength) {

        final boolean[] result = new boolean[expectedLength];

        Arrays.fill(result, false);

        if (values == null) {

            return result;

        }

        for (int i = 0; i < Math.min(values.length, expectedLength); i++) {

            result[i] = Boolean.parseBoolean(values[i] != null ? values[i].trim() : "");

        }

        return result;

    }



    private static String[] booleanArrayToStrings(final boolean[] flags) {

        if (flags == null) {

            return new String[0];

        }

        final String[] result = new String[flags.length];

        for (int i = 0; i < flags.length; i++) {

            result[i] = String.valueOf(flags[i]);

        }

        return result;

    }



    private static String stringProperty(final Map<String, Object> props, final String key) {

        final Object value = props.get(key);

        return value == null ? "" : String.valueOf(value).trim();

    }



    private static String[] stringArrayProperty(final Map<String, Object> props, final String key) {

        final Object value = props.get(key);

        if (value == null) {

            return new String[0];

        }

        if (value instanceof String[]) {

            return copy((String[]) value);

        }

        if (value instanceof String) {

            final String s = ((String) value).trim();

            return s.isEmpty() ? new String[0] : new String[] {s};

        }

        if (value instanceof Object[]) {

            final List<String> list = new ArrayList<>();

            for (final Object item : (Object[]) value) {

                if (item != null) {

                    final String s = String.valueOf(item).trim();

                    if (!s.isEmpty()) {

                        list.add(s);

                    }

                }

            }

            return list.toArray(new String[0]);

        }

        return new String[0];

    }



    private static String[] readPathArray(final SlingHttpServletRequest request, final String fieldName) {

        final Set<String> ordered = new LinkedHashSet<>();

        addAllParameters(request, fieldName, ordered);

        addAllParameters(request, "./" + fieldName, ordered);

        collectMultifieldPathParameters(request, fieldName, ordered);

        return ordered.toArray(new String[0]);

    }



    private static void addAllParameters(

            final SlingHttpServletRequest request, final String name, final Set<String> target) {

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

            final SlingHttpServletRequest request, final String multifieldRoot, final Set<String> target) {

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



    private static String trimToEmpty(final String value) {

        return value == null ? "" : value.trim();

    }



    private static String[] copy(final String[] source) {

        return source == null ? new String[0] : Arrays.copyOf(source, source.length);

    }

}

