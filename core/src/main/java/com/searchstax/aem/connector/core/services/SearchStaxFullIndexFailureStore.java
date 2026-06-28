package com.searchstax.aem.connector.core.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persists metadata for full-index batches that exhausted retryable POST attempts.
 * Production: JCR under {@link #JCR_RUNS_PATH}. Unit tests: optional filesystem {@link Path}.
 */
@Component(service = SearchStaxFullIndexFailureStore.class)
public class SearchStaxFullIndexFailureStore {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxFullIndexFailureStore.class);

    /** Repository path (CRX), not the OS filesystem. */
    public static final String JCR_RUNS_PATH = "/var/searchstaxconnector/fullindex/failure";

    private static final int MAX_ERROR_MESSAGE_CHARS = 8 * 1024;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Reference
    private ResolverUtil resolverUtil;

    /** Non-null only in unit tests (filesystem fallback). */
    private final Path filesystemBaseDir;

    /** OSGi component constructor. */
    public SearchStaxFullIndexFailureStore() {
        this.filesystemBaseDir = null;
    }

    /** Filesystem store for unit tests only. */
    public SearchStaxFullIndexFailureStore(final Path filesystemBaseDir) {
        this.filesystemBaseDir = filesystemBaseDir;
    }

    public void recordFailure(final FailureRecord record) throws IOException {
        if (filesystemBaseDir != null) {
            recordFailureFilesystem(record);
            return;
        }
        recordFailureJcr(record);
    }

    /**
     * Returns batch failure records with {@code timestamp >= since}. Used after a full-index run to scope failures
     * to the current run.
     */
    public List<FailureRecord> listFailuresSince(final Instant since) throws IOException {
        if (filesystemBaseDir != null) {
            return listFailuresFilesystemSince(since);
        }
        return listFailuresJcrSince(since);
    }

    /**
     * Flattens persisted full-index failure records into report rows (one row per path).
     */
    public List<Map<String, Object>> listFailureEventsForReport(
            final String statusFilter, final int maxResults, final int retentionHours) throws IOException {
        if (!matchesStatusFilter(statusFilter, "FAILURE")) {
            return List.of();
        }
        final Instant since = Instant.now().minus(retentionHours, ChronoUnit.HOURS);
        final List<FailureRecord> records = listFailuresSince(since);
        records.sort(Comparator.comparing(FailureRecord::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        final List<Map<String, Object>> events = new ArrayList<>();
        for (final FailureRecord record : records) {
            final List<String> paths = record.getPaths();
            if (paths == null || paths.isEmpty()) {
                events.add(toReportEvent(record, record.getBatchId()));
                continue;
            }
            for (final String path : paths) {
                events.add(toReportEvent(record, path));
            }
        }

        if (events.size() <= maxResults) {
            return events;
        }
        return new ArrayList<>(events.subList(0, maxResults));
    }

    /**
     * Removes all persisted full-index failure records shown in the report.
     *
     * @return number of failure records removed
     */
    public int clearAllFailures() throws IOException {
        if (filesystemBaseDir != null) {
            return clearAllFailuresFilesystem();
        }
        return clearAllFailuresJcr();
    }

    private int clearAllFailuresJcr() throws IOException {
        if (resolverUtil == null) {
            throw new IOException("ResolverUtil not available for JCR failure persistence");
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource runsRoot = resolver.getResource(JCR_RUNS_PATH);
            if (runsRoot == null) {
                return 0;
            }

            int removed = 0;
            for (final Resource child : runsRoot.getChildren()) {
                resolver.delete(child);
                removed++;
            }

            if (removed > 0) {
                resolver.commit();
                LOG.info("Cleared {} full index failure records from report", removed);
            }
            return removed;
        } catch (final LoginException | PersistenceException e) {
            throw new IOException("Failed to clear full index failures from " + JCR_RUNS_PATH, e);
        }
    }

    private int clearAllFailuresFilesystem() throws IOException {
        if (!Files.isDirectory(filesystemBaseDir)) {
            return 0;
        }

        int removed = 0;
        try (Stream<Path> files = Files.list(filesystemBaseDir)) {
            for (final Path file : files.collect(Collectors.toList())) {
                Files.deleteIfExists(file);
                removed++;
            }
        }
        if (removed > 0) {
            LOG.info("Cleared {} full index failure records from filesystem report store", removed);
        }
        return removed;
    }

    private static Map<String, Object> toReportEvent(final FailureRecord record, final String path) {
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("timestamp", formatInstantTimestamp(record.getTimestamp()));
        row.put("path", path != null ? path : "");
        row.put("action", "FULL_REINDEX");
        row.put("status", "FAILURE");
        row.put(
                "message",
                FullIndexReportMessageFormatter.formatFailureMessage(
                        record.getBatchId(),
                        record.getStatusCode(),
                        record.getErrorMessage(),
                        record.getRetryAttempts()));
        row.put("batchId", record.getBatchId() != null ? record.getBatchId() : "");
        row.put("failureKind", resolveFailureKind(record.getBatchId()));
        return row;
    }

    private static String resolveFailureKind(final String batchId) {
        if (batchId != null && batchId.startsWith("path-")) {
            return "PATH";
        }
        return "BATCH";
    }

    private static boolean matchesStatusFilter(final String statusFilter, final String status) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            return true;
        }
        return statusFilter.equalsIgnoreCase(status);
    }

    private static String formatInstantTimestamp(final String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        final ZonedDateTime zoned = Instant.parse(timestamp).atZone(ZoneId.systemDefault());
        return String.format(
                "%04d-%02d-%02d %02d:%02d:%02d",
                zoned.getYear(),
                zoned.getMonthValue(),
                zoned.getDayOfMonth(),
                zoned.getHour(),
                zoned.getMinute(),
                zoned.getSecond());
    }

    private List<FailureRecord> listFailuresJcrSince(final Instant since) throws IOException {
        if (resolverUtil == null) {
            throw new IOException("ResolverUtil not available for JCR failure read");
        }
        final List<FailureRecord> results = new ArrayList<>();
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource parent = resolver.getResource(JCR_RUNS_PATH);
            if (parent == null) {
                return results;
            }
            for (final Resource child : parent.getChildren()) {
                final FailureRecord record = failureRecordFromResource(child);
                if (record != null && isOnOrAfter(record.getTimestamp(), since)) {
                    results.add(record);
                }
            }
        } catch (final LoginException e) {
            throw new IOException("Failed to read full index failures from " + JCR_RUNS_PATH, e);
        }
        return results;
    }

    private List<FailureRecord> listFailuresFilesystemSince(final Instant since) throws IOException {
        if (!Files.exists(filesystemBaseDir)) {
            return List.of();
        }
        final List<FailureRecord> results = new ArrayList<>();
        try (Stream<Path> stream = Files.list(filesystemBaseDir)) {
            final List<Path> jsonFiles =
                    stream.filter(path -> path.toString().endsWith(".json")).collect(Collectors.toList());
            for (final Path file : jsonFiles) {
                final FailureRecord record = OBJECT_MAPPER.readValue(file.toFile(), FailureRecord.class);
                if (isOnOrAfter(record.getTimestamp(), since)) {
                    results.add(record);
                }
            }
        }
        return results;
    }

    private FailureRecord failureRecordFromResource(final Resource resource) {
        final ValueMap properties = resource.getValueMap();
        final String failureJson = properties.get("failureJson", String.class);
        if (failureJson != null && !failureJson.isEmpty()) {
            try {
                return OBJECT_MAPPER.readValue(failureJson, FailureRecord.class);
            } catch (final IOException e) {
                LOG.warn("Could not parse failureJson on {}", resource.getPath(), e);
            }
        }
        final FailureRecord record = new FailureRecord();
        record.setBatchId(properties.get("batchId", String.class));
        record.setStatusCode(intProperty(properties, "statusCode", 0));
        record.setErrorMessage(properties.get("errorMessage", String.class));
        record.setPayloadBytes(intProperty(properties, "payloadBytes", 0));
        record.setTimestamp(properties.get("timestamp", String.class));
        record.setRetryAttempts(intProperty(properties, "retryAttempts", 0));
        final String[] paths = properties.get("paths", String[].class);
        if (paths != null && paths.length > 0) {
            record.setPaths(Arrays.asList(paths));
        }
        return record;
    }

    private static boolean isOnOrAfter(final String timestamp, final Instant since) {
        if (since == null) {
            return true;
        }
        if (timestamp == null || timestamp.isEmpty()) {
            return false;
        }
        return !Instant.parse(timestamp).isBefore(since);
    }

    private static int intProperty(final ValueMap properties, final String name, final int defaultValue) {
        final Integer value = properties.get(name, Integer.class);
        return value != null ? value : defaultValue;
    }

    private void recordFailureJcr(final FailureRecord record) throws IOException {
        if (resolverUtil == null) {
            throw new IOException("ResolverUtil not available for JCR failure persistence");
        }
        final String timestamp = record.getTimestamp();
        final String nodeName = buildNodeName(record.getBatchId(), toEpochMilli(timestamp));

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            ensureRunsFolder(resolver);
            final Resource parent = resolver.getResource(JCR_RUNS_PATH);
            if (parent == null) {
                throw new IOException("Runs folder missing after ensure: " + JCR_RUNS_PATH);
            }

            final Map<String, Object> props = new HashMap<>();
            props.put("jcr:primaryType", "nt:unstructured");
            props.put("batchId", record.getBatchId());
            props.put("statusCode", record.getStatusCode());
            props.put("errorMessage", record.getErrorMessage());
            props.put("payloadBytes", record.getPayloadBytes());
            props.put("timestamp", timestamp);
            props.put("retryAttempts", record.getRetryAttempts());
            props.put("failureJson", OBJECT_MAPPER.writeValueAsString(toJsonMap(record, timestamp)));

            final List<String> paths = record.getPaths();
            if (paths != null && !paths.isEmpty()) {
                props.put("paths", paths.toArray(new String[0]));
            }

            resolver.create(parent, nodeName, props);
            resolver.commit();
            LOG.info("Full index failure recorded: {}/{}", JCR_RUNS_PATH, nodeName);
        } catch (final LoginException | PersistenceException e) {
            throw new IOException("Failed to persist full index failure to " + JCR_RUNS_PATH, e);
        }
    }

    private void recordFailureFilesystem(final FailureRecord record) throws IOException {
        Files.createDirectories(filesystemBaseDir);
        final String timestamp = record.getTimestamp();
        final String fileName = buildNodeName(record.getBatchId(), toEpochMilli(timestamp)) + ".json";
        final Path target = filesystemBaseDir.resolve(fileName).toAbsolutePath();
        OBJECT_MAPPER.writeValue(target.toFile(), toJsonMap(record, timestamp));
        LOG.info("Full index failure recorded (filesystem): {}", target);
    }

    private static void ensureRunsFolder(final ResourceResolver resolver) throws PersistenceException {
        if (resolver.getResource(JCR_RUNS_PATH) != null) {
            return;
        }
        createFolderIfMissing(resolver, "/var", "searchstaxconnector");
        createFolderIfMissing(resolver, "/var/searchstaxconnector", "fullindex");
        createFolderIfMissing(resolver, "/var/searchstaxconnector/fullindex", "failure");
        if (resolver.hasChanges()) {
            resolver.commit();
        }
    }

    private static void createFolderIfMissing(
            final ResourceResolver resolver, final String parentPath, final String name)
            throws PersistenceException {
        final Resource parent = resolver.getResource(parentPath);
        if (parent == null) {
            LOG.warn(
                    "Cannot create folder {}/{}: parent {} is missing or not readable by service user",
                    parentPath,
                    name,
                    parentPath);
            return;
        }
        final String childPath = parentPath + "/" + name;
        if (resolver.getResource(childPath) != null) {
            return;
        }
        final Map<String, Object> props = new HashMap<>();
        props.put("jcr:primaryType", "sling:Folder");
        resolver.create(parent, name, props);
    }

    private static String buildNodeName(final String batchId, final long epochMilli) {
        return String.format("batch-%s-%d", sanitizeBatchId(batchId), epochMilli);
    }

    private static Map<String, Object> toJsonMap(final FailureRecord record, final String timestamp) {
        final Map<String, Object> json = new LinkedHashMap<>();
        json.put("batchId", record.getBatchId());
        json.put("paths", record.getPaths());
        json.put("statusCode", record.getStatusCode());
        json.put("errorMessage", record.getErrorMessage());
        json.put("payloadBytes", record.getPayloadBytes());
        json.put("timestamp", timestamp);
        json.put("retryAttempts", record.getRetryAttempts());
        return json;
    }

    private static long toEpochMilli(final String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return System.currentTimeMillis();
        }
        return Instant.parse(timestamp).toEpochMilli();
    }

    private static String sanitizeBatchId(final String batchId) {
        return batchId == null ? "unknown" : batchId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static String truncateErrorMessage(final String message) {
        if (message == null) {
            return "";
        }
        if (message.length() <= MAX_ERROR_MESSAGE_CHARS) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_CHARS);
    }

    public static final class FailureRecord {

        private String batchId;
        private List<String> paths;
        private int statusCode;
        private String errorMessage;
        private int payloadBytes;
        private String timestamp;
        private int retryAttempts;

        public FailureRecord() {
        }

        public FailureRecord(
                final String batchId,
                final List<String> paths,
                final int statusCode,
                final String errorMessage,
                final int payloadBytes,
                final Instant timestamp,
                final int retryAttempts) {
            this.batchId = batchId;
            this.paths = paths;
            this.statusCode = statusCode;
            this.errorMessage = truncateErrorMessage(errorMessage);
            this.payloadBytes = payloadBytes;
            this.timestamp = timestamp == null ? "" : timestamp.toString();
            this.retryAttempts = retryAttempts;
        }

        public String getBatchId() {
            return batchId;
        }

        public void setBatchId(final String batchId) {
            this.batchId = batchId;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(final List<String> paths) {
            this.paths = paths;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(final int statusCode) {
            this.statusCode = statusCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(final String errorMessage) {
            this.errorMessage = truncateErrorMessage(errorMessage);
        }

        public int getPayloadBytes() {
            return payloadBytes;
        }

        public void setPayloadBytes(final int payloadBytes) {
            this.payloadBytes = payloadBytes;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(final String timestamp) {
            this.timestamp = timestamp;
        }

        public int getRetryAttempts() {
            return retryAttempts;
        }

        public void setRetryAttempts(final int retryAttempts) {
            this.retryAttempts = retryAttempts;
        }
    }
}
