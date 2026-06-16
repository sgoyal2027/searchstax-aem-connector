package com.searchstax.aem.connector.core.services.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalQueueServiceImplTest {

    @Test
    void nodeNameForPath_sanitizesSpecialCharacters() {
        final String nodeName =
                IncrementalQueueServiceImpl.nodeNameForPath("/content/wknd/en/my page");

        assertFalse(nodeName.contains(" "));
        assertFalse(nodeName.contains("/"));
    }

    @Test
    void nodeNameForPath_truncatesVeryLongPaths() {
        final String longPath = "/content/" + "a".repeat(250);
        final String nodeName = IncrementalQueueServiceImpl.nodeNameForPath(longPath);

        assertTrue(nodeName.length() <= 200);
    }
}
