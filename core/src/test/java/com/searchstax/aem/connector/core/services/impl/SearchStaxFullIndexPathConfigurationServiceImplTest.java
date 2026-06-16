package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxFullIndexPathConfigurationServiceImplTest {

    private SearchStaxFullIndexPathConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchStaxFullIndexPathConfigurationServiceImpl();
    }

    @Test
    void resolveEffectiveIncludes_returnsValidIncludesUnderRoot() {
        final FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "/content/mysite",
                        new String[] {"/content/mysite/us", "/content/mysite/uk"},
                        new boolean[] {true, true},
                        new String[0]);

        assertArrayEquals(
                new String[] {"/content/mysite/us", "/content/mysite/uk"},
                service.resolveEffectiveIncludes(config));
    }

    @Test
    void resolveEffectiveIncludes_emptyWhenIncludeOutsideRoot() {
        final FullIndexPathConfig config =
                new FullIndexPathConfig("/content/site", new String[] {"/etc"}, new boolean[] {true}, new String[0]);

        assertEquals(0, service.resolveEffectiveIncludes(config).length);
    }

    @Test
    void resolveEffectiveIncludes_dedupesTrailingSlashVariants() {
        final FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "/content/site",
                        new String[] {"/content/site/us/", "/content/site/us"},
                        new boolean[] {false, false},
                        new String[0]);

        assertArrayEquals(new String[] {"/content/site/us"}, service.resolveEffectiveIncludes(config));
    }

    @Test
    void resolveEffectiveExcludes_filtersOutsideRoot() {
        final FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "/content/mysite",
                        new String[] {"/content/mysite/us"},
                        new boolean[] {true},
                        new String[] {"/content/mysite/us/blog/test", "/etc/exclude"});

        assertArrayEquals(
                new String[] {"/content/mysite/us/blog/test"},
                service.resolveEffectiveExcludes(config));
    }

    @Test
    void isExcludedPath_hierarchicalMatch_doesNotFalsePositiveOnSiblingPath() {
        final String[] excludes = new String[] {"/content/site/page1"};

        assertTrue(service.isExcludedPath("/content/site/page1", excludes));
        assertFalse(service.isExcludedPath("/content/site/page10", excludes));
    }

    @Test
    void isExcludedPath_hierarchicalMatch_excludesDescendants() {
        final String[] excludes = new String[] {"/content/site/page1"};

        assertTrue(service.isExcludedPath("/content/site/page1/child", excludes));
    }

    @Test
    void isPathUnder_distinguishesSiblingPaths() {
        assertTrue(SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(
                "/content/site/page1", "/content/site/page1"));
        assertTrue(SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(
                "/content/site/page1/child", "/content/site/page1"));
        assertFalse(SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(
                "/content/site/page10", "/content/site/page1"));
    }

}
