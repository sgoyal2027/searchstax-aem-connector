package com.searchstax.aem.connector.core.servlets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchStaxFullIndexStatusServletTest {

    @Test
    void abbreviateMiddle_returnsOriginalWhenWithinLimit() {
        assertEquals("/content/wknd/en/page", SearchStaxFullIndexStatusServlet.abbreviateMiddle("/content/wknd/en/page", 120));
    }

    @Test
    void abbreviateMiddle_shortensLongPaths() {
        final String path = "/content/wknd/us/en/magazine/adventures/alaska-adventure/details/very-long-segment";
        final String abbreviated = SearchStaxFullIndexStatusServlet.abbreviateMiddle(path, 40);

        assertEquals(40, abbreviated.length());
        assertTrueContains(abbreviated, "...");
    }

    @Test
    void abbreviateMiddle_handlesNullAndEmpty() {
        assertEquals("", SearchStaxFullIndexStatusServlet.abbreviateMiddle(null, 40));
        assertEquals("", SearchStaxFullIndexStatusServlet.abbreviateMiddle("", 40));
        assertEquals("", SearchStaxFullIndexStatusServlet.abbreviateMiddle(null, 0));
    }

    private static void assertTrueContains(final String value, final String fragment) {
        org.junit.jupiter.api.Assertions.assertTrue(value.contains(fragment));
    }
}
