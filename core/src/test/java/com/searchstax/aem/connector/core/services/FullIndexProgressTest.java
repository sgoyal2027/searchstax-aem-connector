package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
