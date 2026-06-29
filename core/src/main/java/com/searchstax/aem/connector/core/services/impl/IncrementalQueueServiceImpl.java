package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component(service = IncrementalQueueService.class)
public class IncrementalQueueServiceImpl implements IncrementalQueueService {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalQueueServiceImpl.class);

    static final String PENDING_QUEUE_PATH = "/var/searchstaxconnector/incremental-index/pending";

    private static final String PROP_PATH = "path";
    private static final String PROP_ACTION_TYPE = "actionType";
    private static final String PROP_EVENT_TIME = "eventTime";
    private static final String PROP_RETRY_COUNT = "retryCount";
    private static final String PROP_CORRELATION_ID = "correlationId";

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public void addRequest(
            final String path,
            final ReplicationActionType actionType,
            final String correlationId) {
        if (path == null || path.isBlank() || actionType == null) {
            LOG.warn("Skipping invalid queue request. Path={}, Action={}", path, actionType);
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource pendingRoot = ensurePendingRoot(resolver);
            final Resource existing = findNodeByPath(pendingRoot, path);

            final Map<String, Object> properties = new HashMap<>();
            properties.put(PROP_PATH, path);
            properties.put(PROP_ACTION_TYPE, actionType.name());
            properties.put(PROP_EVENT_TIME, System.currentTimeMillis());
            if (correlationId != null && !correlationId.isBlank()) {
                properties.put(PROP_CORRELATION_ID, correlationId);
            }

            if (existing != null) {
                final ModifiableValueMap mvm = existing.adaptTo(ModifiableValueMap.class);
                if (mvm != null) {
                    mvm.put(PROP_ACTION_TYPE, actionType.name());
                    mvm.put(PROP_EVENT_TIME, System.currentTimeMillis());
                    if (correlationId != null && !correlationId.isBlank()) {
                        mvm.put(PROP_CORRELATION_ID, correlationId);
                    }
                }
            } else {
                resolver.create(pendingRoot, nodeNameForPath(path), properties);
            }

            resolver.commit();
            LOG.debug("Request added to JCR queue. Path={} Action={} QueueSize={}", path, actionType, size(resolver));
        } catch (Exception e) {
            LOG.error("Unable to add request to JCR queue. Path={}", path, e);
        }
    }

    @Override
    public List<IndexRequest> getBatch(final int batchSize) {
        final List<IndexRequest> batch = new ArrayList<>();
        if (batchSize <= 0) {
            return batch;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource pendingRoot = resolver.getResource(PENDING_QUEUE_PATH);
            if (pendingRoot == null) {
                return batch;
            }

            final List<IndexRequest> all = new ArrayList<>();
            for (final Resource child : pendingRoot.getChildren()) {
                final IndexRequest request = toIndexRequest(child);
                if (request != null) {
                    all.add(request);
                }
            }

            all.sort(Comparator.comparingLong(IndexRequest::getEventTime));
            return all.stream().limit(batchSize).collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Unable to read batch from JCR queue", e);
            return batch;
        }
    }

    @Override
    public void removeProcessed(final List<IndexRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource pendingRoot = resolver.getResource(PENDING_QUEUE_PATH);
            if (pendingRoot == null) {
                return;
            }

            int removed = 0;
            for (final IndexRequest request : requests) {
                if (request == null || request.getPath() == null) {
                    continue;
                }
                final Resource node = findNodeByPath(pendingRoot, request.getPath());
                if (node != null) {
                    resolver.delete(node);
                    removed++;
                }
            }

            if (removed > 0) {
                resolver.commit();
            }

            LOG.debug("Removed {} processed requests from JCR queue. Remaining={}", removed, size(resolver));
        } catch (Exception e) {
            LOG.error("Unable to remove processed requests from JCR queue", e);
        }
    }

    @Override
    public void updateRequest(final IndexRequest request) {
        if (request == null || request.getPath() == null) {
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource pendingRoot = resolver.getResource(PENDING_QUEUE_PATH);
            if (pendingRoot == null) {
                return;
            }

            final Resource node = findNodeByPath(pendingRoot, request.getPath());
            if (node == null) {
                return;
            }

            final ModifiableValueMap mvm = node.adaptTo(ModifiableValueMap.class);
            if (mvm != null) {
                mvm.put(PROP_RETRY_COUNT, request.getRetryCount());
                if (request.getActionType() != null) {
                    mvm.put(PROP_ACTION_TYPE, request.getActionType().name());
                }
                resolver.commit();
            }
        } catch (Exception e) {
            LOG.error("Unable to update JCR queue item. Path={}", request.getPath(), e);
        }
    }

    @Override
    public int size() {
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            return size(resolver);
        } catch (Exception e) {
            LOG.error("Unable to read JCR queue size", e);
            return 0;
        }
    }

    @Override
    public void incrementRetryCount(final IndexRequest request) {
        if (request == null) {
            return;
        }
        request.setRetryCount(request.getRetryCount() + 1);
        updateRequest(request);
    }

    @Override
    public int clearPendingQueue() {
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource pendingRoot = resolver.getResource(PENDING_QUEUE_PATH);
            if (pendingRoot == null) {
                return 0;
            }

            final List<Resource> children = new ArrayList<>();
            pendingRoot.getChildren().forEach(children::add);

            int removed = 0;
            for (final Resource child : children) {
                resolver.delete(child);
                removed++;
            }

            if (removed > 0) {
                resolver.commit();
                LOG.info("Cleared {} pending incremental index request(s) from queue", removed);
            }
            return removed;
        } catch (Exception e) {
            LOG.error("Unable to clear incremental pending queue", e);
            return 0;
        }
    }

    private static int size(final ResourceResolver resolver) {
        final Resource pendingRoot = resolver.getResource(PENDING_QUEUE_PATH);
        if (pendingRoot == null) {
            return 0;
        }
        int count = 0;
        final Iterator<Resource> children = pendingRoot.listChildren();
        while (children.hasNext()) {
            children.next();
            count++;
        }
        return count;
    }

    private Resource ensurePendingRoot(final ResourceResolver resolver) throws PersistenceException {
        return ResourceUtil.getOrCreateResource(
                resolver,
                PENDING_QUEUE_PATH,
                "sling:Folder",
                "sling:Folder",
                true);
    }

    private static Resource findNodeByPath(final Resource pendingRoot, final String path) {
        for (final Resource child : pendingRoot.getChildren()) {
            final ValueMap vm = child.getValueMap();
            if (path.equals(vm.get(PROP_PATH, String.class))) {
                return child;
            }
        }
        return null;
    }

    private static IndexRequest toIndexRequest(final Resource node) {
        final ValueMap vm = node.getValueMap();
        final String path = vm.get(PROP_PATH, String.class);
        final String actionTypeName = vm.get(PROP_ACTION_TYPE, String.class);
        if (path == null || actionTypeName == null) {
            return null;
        }

        try {
            final IndexRequest request = new IndexRequest();
            request.setPath(path);
            request.setActionType(ReplicationActionType.valueOf(actionTypeName));
            request.setEventTime(vm.get(PROP_EVENT_TIME, 0L));
            request.setRetryCount(vm.get(PROP_RETRY_COUNT, 0));
            request.setCorrelationId(vm.get(PROP_CORRELATION_ID, String.class));
            return request;
        } catch (IllegalArgumentException e) {
            LOG.warn("Skipping queue item with invalid actionType. Path={} Action={}", path, actionTypeName);
            return null;
        }
    }

    static String nodeNameForPath(final String path) {
        String safe = path.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.length() > 200) {
            safe = safe.substring(0, 100) + "_" + Integer.toHexString(path.hashCode());
        }
        return safe;
    }
}
