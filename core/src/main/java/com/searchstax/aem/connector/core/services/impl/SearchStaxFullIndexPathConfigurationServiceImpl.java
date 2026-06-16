package com.searchstax.aem.connector.core.services.impl;



import com.searchstax.aem.connector.core.services.FullIndexPathConfig;

import com.searchstax.aem.connector.core.services.SearchStaxFullIndexPathConfigurationService;

import org.osgi.service.component.annotations.Component;



import java.util.ArrayList;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Set;



@Component(service = SearchStaxFullIndexPathConfigurationService.class)

public class SearchStaxFullIndexPathConfigurationServiceImpl implements SearchStaxFullIndexPathConfigurationService {



    @Override

    public String[] resolveEffectiveIncludes(final FullIndexPathConfig config) {

        if (config == null) {

            return new String[0];

        }

        final String root = normalizePath(config.getRootPath(), true);

        if (root.isEmpty()) {

            return new String[0];

        }

        return filterPathsUnderRoot(root, normalizeAndDedupe(config.getIncludePaths(), false));

    }



    @Override

    public String[] resolveEffectiveExcludes(final FullIndexPathConfig config) {

        if (config == null) {

            return new String[0];

        }

        final String root = normalizePath(config.getRootPath(), true);

        if (root.isEmpty()) {

            return new String[0];

        }

        return filterPathsUnderRoot(root, normalizeAndDedupe(config.getExcludePaths(), false));

    }



    @Override

    public boolean isExcludedPath(final String path, final String[] effectiveExcludes) {

        if (path == null || path.isEmpty() || effectiveExcludes == null || effectiveExcludes.length == 0) {

            return false;

        }

        for (final String exclude : effectiveExcludes) {

            if (isPathUnder(path, exclude)) {

                return true;

            }

        }

        return false;

    }



    static String normalizePath(final String path, final boolean isRootPath) {

        if (path == null) {

            return "";

        }

        String normalized = path.trim();

        if (normalized.isEmpty() || normalized.contains("//")) {

            return "";

        }

        if (!normalized.startsWith("/")) {

            normalized = "/" + normalized;

        }

        if (!isRootPath && normalized.length() > 1 && normalized.endsWith("/")) {

            normalized = normalized.substring(0, normalized.length() - 1);

        }

        return normalized;

    }



    static String[] normalizeAndDedupe(final String[] paths, final boolean isRootPath) {

        if (paths == null || paths.length == 0) {

            return new String[0];

        }

        final Set<String> ordered = new LinkedHashSet<>();

        for (final String path : paths) {

            final String normalized = normalizePath(path, isRootPath);

            if (!normalized.isEmpty()) {

                ordered.add(normalized);

            }

        }

        return ordered.toArray(new String[0]);

    }



    static boolean isPathUnder(final String path, final String ancestor) {

        if (path == null || ancestor == null || path.isEmpty() || ancestor.isEmpty()) {

            return false;

        }

        return path.equals(ancestor) || path.startsWith(ancestor + "/");

    }



    static String[] filterPathsUnderRoot(final String root, final String[] paths) {

        if (root == null || root.isEmpty() || paths == null || paths.length == 0) {

            return new String[0];

        }

        final List<String> filtered = new ArrayList<>();

        for (final String path : paths) {

            if (isPathUnder(path, root)) {

                filtered.add(path);

            }

        }

        return filtered.toArray(new String[0]);

    }

}

