package com.searchstax.aem.connector.core.models;

import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayloadBatchTest {

    @Test
    void testConstructor() {

        List<IndexRequest> requests = new ArrayList<>();

        PayloadBatch batch = new PayloadBatch(
                requests,
                "{\"test\":true}",
                123L,
                "batch-1");

        assertSame(requests, batch.getRequests());
        assertEquals("{\"test\":true}", batch.getPayload());
        assertEquals(123L, batch.getPayloadSize());
        assertEquals("batch-1", batch.getBatchId());
    }

    @Test
    void testSettersAndGetters() {

        PayloadBatch batch = new PayloadBatch(
                null,
                null,
                0L,
                null);

        List<IndexRequest> requests = new ArrayList<>();

        batch.setRequests(requests);
        batch.setPayload("payload");
        batch.setPayloadSize(999L);
        batch.setBatchId("batch-2");

        assertSame(requests, batch.getRequests());
        assertEquals("payload", batch.getPayload());
        assertEquals(999L, batch.getPayloadSize());
        assertEquals("batch-2", batch.getBatchId());
    }
}