package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.models.PayloadBatch;

import java.util.List;
import java.util.Map;

public interface PayLoadBatchService {

    List<PayloadBatch> buildIndexBatches(
            List<IndexRequest> requests,
            List<Map<String,Object>> documents) throws Exception;

    List<PayloadBatch> buildDeleteBatches(
            List<IndexRequest> requests,
            List<String> ids) throws Exception;
}
