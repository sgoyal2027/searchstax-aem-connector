package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FullIndexProgressTest {

    @Test
    void getTotalAttempted_isSuccessPlusPathFailures() {
        final FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.PARTIAL_FAILURE,
                        48,
                        48,
                        1,
                        9,
                        39,
                        1,
                        "/content/last",
                        0,
                        1000,
                        "done");

        assertEquals(48, progress.getTotalProcessed());
        assertEquals(49, progress.getTotalAttempted());
    }

    @Test
    void constructor_normalizesNullMessageAndPath() {
        final FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.SUCCESS,
                        1,
                        1,
                        0,
                        1,
                        0,
                        1,
                        null,
                        100L,
                        200L,
                        null);

        assertEquals("", progress.getMessage());
        assertEquals("", progress.getLastIndexedPath());
    }

    @Test
    void gettersExposeStoredCounters() {
        final FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.RUNNING,
                        5,
                        4,
                        1,
                        3,
                        2,
                        2,
                        "/content/page",
                        1_000L,
                        500L,
                        "Indexing");

        assertEquals(FullIndexProgress.State.RUNNING, progress.getState());
        assertEquals(4, progress.getSuccessCount());
        assertEquals(1, progress.getFailureCount());
        assertEquals(3, progress.getPagesIndexed());
        assertEquals(2, progress.getAssetsIndexed());
        assertEquals(2, progress.getCurrentBatchNumber());
        assertEquals(1_000L, progress.getStartedAt());
        assertEquals(500L, progress.getElapsedMs());
        assertFalse(progress.getMessage().isEmpty());
    }
}
