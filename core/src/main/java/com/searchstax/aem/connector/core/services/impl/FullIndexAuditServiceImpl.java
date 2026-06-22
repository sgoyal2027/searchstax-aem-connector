package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexAuditService;
import com.searchstax.aem.connector.core.services.FullIndexReportMessageFormatter;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component(service = FullIndexAuditService.class)
public class FullIndexAuditServiceImpl implements FullIndexAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(FullIndexAuditServiceImpl.class);

    static final String AUDIT_ROOT = "/var/searchstaxconnector/fullindex/audit";

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public void recordSuccessBatch(final List<String> paths, final int batchNumber, final long durationMs) {
        if (paths == null || paths.isEmpty()) {
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource auditRoot = ResourceUtil.getOrCreateResource(
                    resolver, AUDIT_ROOT, "sling:Folder", "sling:Folder", true);
            final Calendar timestamp = Calendar.getInstance();
            final String batchId = "batch-" + batchNumber;
            final String message = FullIndexReportMessageFormatter.formatSuccessMessage(batchNumber, durationMs);

            for (final String path : paths) {
                if (path == null || path.isBlank()) {
                    continue;
                }
                final Map<String, Object> properties = new HashMap<>();
                properties.put("jcr:primaryType", "nt:unstructured");
                properties.put("path", path);
                properties.put("action", ACTION_FULL_REINDEX);
                properties.put("status", STATUS_SUCCESS);
                properties.put("message", message);
                properties.put("batchId", batchId);
                properties.put("durationMs", durationMs);
                properties.put("eventKind", "BATCH");
                properties.put("timestamp", timestamp);
                resolver.create(auditRoot, buildEventNodeName(), properties);
            }
            resolver.commit();
            LOG.info("Recorded {} full index success audit events for batch {}", paths.size(), batchNumber);
        } catch (Exception e) {
            LOG.error("Unable to record full index success audit events for batch {}", batchNumber, e);
        }
    }

    @Override
    public List<Map<String, Object>> listEventsForReport(
            final String statusFilter, final int maxResults, final int retentionHours) {
        final List<Map<String, Object>> events = new ArrayList<>();
        final Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.HOUR, -retentionHours);

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
                if (!matchesStatusFilter(statusFilter, status)) {
                    continue;
                }

                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("timestamp", formatTimestamp(timestamp));
                row.put("path", vm.get("path", ""));
                row.put("action", vm.get("action", ACTION_FULL_REINDEX));
                row.put("status", status);
                row.put(
                        "message",
                        FullIndexReportMessageFormatter.formatSuccessMessageFromStored(
                                vm.get("message", ""),
                                vm.get("batchId", ""),
                                longValue(vm.get("durationMs"))));
                row.put("batchId", vm.get("batchId", ""));
                row.put("eventKind", vm.get("eventKind", "BATCH"));
                events.add(row);
            }

            deduplicateEvents(events);
            events.sort(Comparator.comparing(m -> (String) m.get("timestamp"), Comparator.reverseOrder()));
            if (events.size() > maxResults) {
                return new ArrayList<>(events.subList(0, maxResults));
            }
        } catch (Exception e) {
            LOG.error("Unable to list full index audit events", e);
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
                LOG.info("Purged {} full index audit events older than {} hours", removed, hours);
            }
        } catch (Exception e) {
            LOG.error("Unable to purge full index audit events", e);
        }
    }

    private static String buildEventNodeName() {
        return "event-" + UUID.randomUUID().toString().replace("-", "");
    }

    private static long longValue(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private static boolean matchesStatusFilter(final String statusFilter, final String status) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            return true;
        }
        return statusFilter.equalsIgnoreCase(status);
    }

    private static void deduplicateEvents(final List<Map<String, Object>> events) {
        final Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (final Map<String, Object> row : events) {
            final String key = String.format(
                    "%s|%s|%s|%s|%s",
                    row.getOrDefault("path", ""),
                    row.getOrDefault("action", ""),
                    row.getOrDefault("status", ""),
                    row.getOrDefault("timestamp", ""),
                    row.getOrDefault("message", ""));
            unique.putIfAbsent(key, row);
        }
        events.clear();
        events.addAll(unique.values());
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
