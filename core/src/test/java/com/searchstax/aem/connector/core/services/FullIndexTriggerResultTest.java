package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullIndexTriggerResultTest {

    @Test
    void constructor_normalizesNullFields() {
        final FullIndexTriggerResult result = new FullIndexTriggerResult(true, null, null, 202);

        assertTrue(result.isAccepted());
        assertEquals("", result.getJobId());
        assertEquals("", result.getMessage());
        assertEquals(202, result.getHttpStatus());
    }

    @Test
    void constructor_preservesRejectedResult() {
        final FullIndexTriggerResult result =
                new FullIndexTriggerResult(false, "", "A full index job is already running.", 409);

        assertFalse(result.isAccepted());
        assertEquals(409, result.getHttpStatus());
        assertEquals("A full index job is already running.", result.getMessage());
    }
}
