package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.FailedRequestService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(service = FailedRequestService.class)
public class FailedRequestServiceImpl implements FailedRequestService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    FailedRequestServiceImpl.class);

    private static final String INCREMENTAL_INDEX_FAILED_SAVE =
            "/var/searchstaxconnector/incremental-index/failed";

    @Reference
    ResolverUtil resolverUtil;

    @Override
    public void saveFailedRequest(IndexRequest request, String reason,ApiResponse response) {

        try (ResourceResolver resourceResolver =
                     resolverUtil.getServiceResolver()) {

            Resource failedRoot =
                    ResourceUtil.getOrCreateResource(
                            resourceResolver,
                            INCREMENTAL_INDEX_FAILED_SAVE,
                            "sling:Folder",
                            "sling:Folder",
                            true);

            String nodeName =
                    "item-" + UUID.randomUUID();

            Map<String, Object> properties =
                    new HashMap<>();

            properties.put(
                    "batchId",
                    request.getBatchId());

            properties.put(
                    "path",
                    request.getPath());

            properties.put(
                    "actionType",
                    request.getActionType()!=null
                        ? request.getActionType().name()
                        : "Unknown");

            properties.put(
                    "retryCount",
                    request.getRetryCount());

            properties.put(
                    "failureReason",
                    reason);

            properties.put(
                    "failedAt",
                    Calendar.getInstance());

            properties.put(
                    "statusCode",
                    response != null
                        ? response.getStatusCode()
                        : -1);

            properties.put(
                    "responseMessage",
                    response != null
                        ? response.getResponseBody()
                        : "N/A");

            resourceResolver.create(
                    failedRoot,
                    nodeName,
                    properties);

            resourceResolver.commit();

            LOG.info(
                    "Failed request saved. BatchId={} Path={} Reason={}",
                    request.getBatchId(),
                    request.getPath(),
                    reason);

        } catch (Exception e) {

            LOG.error(
                    "Unable to save failed request. Path={}",
                    request.getPath(),
                    e);
        }
    }
}
