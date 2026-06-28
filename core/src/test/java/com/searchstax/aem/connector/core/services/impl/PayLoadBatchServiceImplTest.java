package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.models.PayloadBatch;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PayLoadBatchServiceImplTest {

    private final PayLoadBatchServiceImpl service =
            new PayLoadBatchServiceImpl();

    @Test
    void testBuildIndexBatchesSizeMismatch() {

        List<IndexRequest> requests =
                Collections.singletonList(newRequest("/content/page1"));

        List<Map<String, Object>> documents =
                Collections.emptyList();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.buildIndexBatches(requests, documents));
    }

    @Test
    void testBuildIndexBatchesSingleBatch() throws Exception {

        List<IndexRequest> requests = new ArrayList<>();
        List<Map<String, Object>> documents = new ArrayList<>();

        requests.add(newRequest("/content/page1"));
        documents.add(newDocument("/content/page1"));

        List<PayloadBatch> result =
                service.buildIndexBatches(requests, documents);

        assertEquals(1, result.size());

        PayloadBatch batch = result.get(0);

        assertEquals(1, batch.getRequests().size());
        assertNotNull(batch.getBatchId());
        assertFalse(batch.getPayload().isEmpty());
        assertTrue(batch.getPayloadSize() > 0);

        assertEquals(
                batch.getBatchId(),
                requests.get(0).getBatchId());
    }

    @Test
    void testBuildIndexBatchesMultipleBatches() throws Exception {

        List<IndexRequest> requests = new ArrayList<>();
        List<Map<String, Object>> documents = new ArrayList<>();

        int total =
                SearchStaxIndexingLimits.MAX_BATCH_DOCUMENT_COUNT + 1;

        for (int i = 0; i < total; i++) {

            requests.add(newRequest("/content/page" + i));
            documents.add(newDocument("/content/page" + i));
        }

        List<PayloadBatch> result =
                service.buildIndexBatches(requests, documents);

        assertEquals(2, result.size());
    }

    @Test
    void testBuildDeleteBatchesEmpty() throws Exception {

        List<PayloadBatch> result =
                service.buildDeleteBatches(
                        Collections.emptyList(),
                        Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildDeleteBatchesSingleBatch() throws Exception {

        List<IndexRequest> requests =
                Collections.singletonList(
                        newRequest("/content/page1"));

        List<String> ids =
                Collections.singletonList("/content/page1");

        List<PayloadBatch> result =
                service.buildDeleteBatches(requests, ids);

        assertEquals(1, result.size());

        PayloadBatch batch =
                result.get(0);

        assertTrue(batch.getPayload().contains("delete"));
        assertTrue(batch.getPayload().contains("/content/page1"));
        assertNotNull(batch.getBatchId());

        assertEquals(
                batch.getBatchId(),
                requests.get(0).getBatchId());
    }

    @Test
    void testBuildDeleteBatchesMultipleBatches() throws Exception {

        List<IndexRequest> requests =
                new ArrayList<>();

        List<String> ids =
                new ArrayList<>();

        int total =
                SearchStaxIndexingLimits.MAX_BATCH_DOCUMENT_COUNT + 1;

        for (int i = 0; i < total; i++) {

            requests.add(newRequest("/content/page" + i));
            ids.add("/content/page" + i);
        }

        List<PayloadBatch> result =
                service.buildDeleteBatches(requests, ids);

        assertEquals(2, result.size());
    }

    @Test
    void testBuildDeleteBatchesBatchIdAssigned() throws Exception {

        List<IndexRequest> requests =
                Collections.singletonList(
                        newRequest("/content/test"));

        List<String> ids =
                Collections.singletonList("/content/test");

        service.buildDeleteBatches(requests, ids);

        assertNotNull(requests.get(0).getBatchId());
    }

    private IndexRequest newRequest(String path) {

        IndexRequest request =
                new IndexRequest();

        request.setPath(path);

        return request;
    }

    private Map<String, Object> newDocument(String id) {

        Map<String, Object> map =
                new HashMap<>();

        map.put("id", id);
        map.put("title", "Title");
        map.put("content", "Sample Content");

        return map;
    }
}