package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component(service = IndexingAuditService.class)
public class IndexingAuditServiceImpl implements IndexingAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingAuditServiceImpl.class);

    static final String AUDIT_ROOT = "/var/searchstaxconnector/incremental-index/audit";

    private static final String FAILED_ROOT = "/var/searchstaxconnector/incremental-index/failed";

    @Reference
    private ResolverUtil resolverUtil;

    @Reference
    private IncrementalQueueService incrementalQueueService;

    @Override
    public void recordEvent(
            final String path,
            final String action,
            final String status,
            final String message,
            final String correlationId,
            final long durationMs,
            final String batchId) {

        if (path == null || path.isBlank()) {
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource auditRoot = ResourceUtil.getOrCreateResource(
                    resolver, AUDIT_ROOT, "sling:Folder", "sling:Folder", true);

            final Map<String, Object> properties = new HashMap<>();
            properties.put("path", path);
            properties.put("action", action != null ? action : "");
            properties.put("status", status != null ? status : "");
            properties.put("message", message != null ? message : "");
            properties.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
            properties.put("durationMs", durationMs);
            properties.put("batchId", batchId != null ? batchId : "");
            properties.put("timestamp", Calendar.getInstance());

            resolver.create(auditRoot, "event-" + UUID.randomUUID(), properties);
            resolver.commit();
        } catch (Exception e) {
            LOG.error("Unable to record indexing audit event. Path={} Status={}", path, status, e);
        }
    }

    @Override
    public List<Map<String, Object>> listEvents(
            final String statusFilter,
            final String actionFilter,
            final boolean excludeQueued,
            final int maxResults) {
        final List<Map<String, Object>> events = new ArrayList<>();
        final Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.HOUR, -24);

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource auditRoot = resolver.getResource(AUDIT_ROOT);
            if (auditRoot == null) {
                return events;
            }

            for (final Resource child : auditRoot.getChildren()) {
                final ValueMap vm = child.getValueMap();
                final Calendar timestamp = vm.get("timestamp", Calendar.class);
                if (timestamp != null && timestamp.before(cutoff)) {
                    continue;
                }

                final String status = vm.get("status", "");
                if (statusFilter != null
                        && !statusFilter.isBlank()
                        && !"ALL".equalsIgnoreCase(statusFilter)
                        && !statusFilter.equalsIgnoreCase(status)) {
                    continue;
                }
                if (excludeQueued && STATUS_QUEUED.equalsIgnoreCase(status)) {
                    continue;
                }

                final String action = vm.get("action", "");
                if (actionFilter != null
                        && !actionFilter.isBlank()
                        && !"ALL".equalsIgnoreCase(actionFilter)
                        && !actionFilter.equalsIgnoreCase(action)) {
                    continue;
                }

                final Map<String, Object> row = new HashMap<>();
                row.put("timestamp", formatTimestamp(timestamp));
                row.put("path", vm.get("path", ""));
                row.put("action", action);
                row.put("status", status);
                row.put("duration", vm.get("durationMs", 0L));
                row.put("message", vm.get("message", ""));
                events.add(row);
            }

            events.sort(Comparator.comparing(m -> (String) m.get("timestamp"), Comparator.reverseOrder()));
            if (events.size() > maxResults) {
                return new ArrayList<>(events.subList(0, maxResults));
            }
        } catch (Exception e) {
            LOG.error("Unable to list indexing audit events", e);
        }

        return events;
    }

    @Override
    public void purgeOlderThanHours(final int hours) {
        final Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.HOUR, -hours);

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource auditRoot = resolver.getResource(AUDIT_ROOT);
            if (auditRoot == null) {
                return;
            }

            int removed = 0;
            for (final Resource child : auditRoot.getChildren()) {
                final Calendar timestamp = child.getValueMap().get("timestamp", Calendar.class);
                if (timestamp != null && timestamp.before(cutoff)) {
                    resolver.delete(child);
                    removed++;
                }
            }

            if (removed > 0) {
                resolver.commit();
                LOG.info("Purged {} indexing audit events older than {} hours", removed, hours);
            }
        } catch (Exception e) {
            LOG.error("Unable to purge indexing audit events", e);
        }
    }

    @Override
    public void reprocessFailedPath(final String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        ReplicationActionType actionType = ReplicationActionType.ACTIVATE;

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource failedRoot = resolver.getResource(FAILED_ROOT);
            if (failedRoot != null) {
                for (final Resource child : failedRoot.getChildren()) {
                    final ValueMap vm = child.getValueMap();
                    if (path.equals(vm.get("path", String.class))) {
                        final String action = vm.get("actionType", ReplicationActionType.ACTIVATE.name());
                        try {
                            actionType = ReplicationActionType.valueOf(action);
                        } catch (IllegalArgumentException ignored) {
                            actionType = ReplicationActionType.ACTIVATE;
                        }
                        resolver.delete(child);
                        break;
                    }
                }
                resolver.commit();
            }
        } catch (Exception e) {
            LOG.warn("Could not remove failed record for reprocess. Path={}", path, e);
        }

        incrementalQueueService.addRequest(path, actionType);
        recordEvent(path, actionType.name(), STATUS_QUEUED, "Reprocess requested", null, 0, null);
        LOG.info("Re-queued failed path for incremental indexing. Path={}", path);
    }

    private static String formatTimestamp(final Calendar timestamp) {
        if (timestamp == null) {
            return "";
        }
        return String.format(
                "%04d-%02d-%02d %02d:%02d:%02d",
                timestamp.get(Calendar.YEAR),
                timestamp.get(Calendar.MONTH) + 1,
                timestamp.get(Calendar.DAY_OF_MONTH),
                timestamp.get(Calendar.HOUR_OF_DAY),
                timestamp.get(Calendar.MINUTE),
                timestamp.get(Calendar.SECOND));
    }
}
