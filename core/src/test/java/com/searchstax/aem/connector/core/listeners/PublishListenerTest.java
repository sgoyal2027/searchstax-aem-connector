package com.searchstax.aem.connector.core.listeners;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.osgi.service.event.Event;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublishListenerTest {

    @InjectMocks
    private PublishListener listener;

    @Mock
    private JobManager jobManager;

    @Mock
    private SlingSettingsService slingSettings;

    @Mock
    private InitialSetupConfigService initialSetupConfigService;

    @Mock
    private Job job;

    private InitialSetupConfig config;

    @BeforeEach
    void setup() {

        config = new InitialSetupConfig();
        config.setEnableConnector(true);
        config.setRootPaths(new String[]{"/content"});
        config.setExcludePaths(new String[0]);
    }

    @Test
    void testActivate() {

        listener.activate();
    }

    @Test
    void testPublishInstance() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("publish"));

        listener.handleEvent(mock(Event.class));

        verifyNoInteractions(jobManager);
    }

    @Test
    void testNullReplicationAction() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        Dictionary<String, Object> props = new Hashtable<>();

        Event event = new Event(
                "dummy/topic",
                props);

        listener.handleEvent(event);

        verifyNoInteractions(jobManager);
    }

    private Event createReplicationEvent(
            String path,
            ReplicationActionType type) {

        ReplicationAction action =
                new ReplicationAction(type, path);

        Event replicationEvent = action.toEvent(false);

        Dictionary<String, Object> props = new Hashtable<>();

        for (String name : replicationEvent.getPropertyNames()) {
            props.put(name, replicationEvent.getProperty(name));
        }

        return new Event(
                ReplicationAction.EVENT_TOPIC,
                props);
    }

    @Test
    void testUnsupportedAction() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        Event event =
                createReplicationEvent(
                        "/content/test",
                        ReplicationActionType.TEST);

        listener.handleEvent(event);

        verifyNoInteractions(jobManager);
    }

    @Test
    void testBlankPath() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        Event event =
                createReplicationEvent(
                        "",
                        ReplicationActionType.ACTIVATE);

        listener.handleEvent(event);

        verifyNoInteractions(jobManager);
    }

    @Test
    void handleEvent_queuesJobForValidActivateOnAuthor() {
        when(slingSettings.getRunModes()).thenReturn(Set.of("author"));
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);
        when(jobManager.addJob(eq("searchstaxconnector/incremental-index"), anyMap())).thenReturn(job);

        listener.handleEvent(createReplicationEvent("/content/wknd/en/page", ReplicationActionType.ACTIVATE));

        verify(jobManager).addJob(eq("searchstaxconnector/incremental-index"), argThat(props ->
                "/content/wknd/en/page".equals(props.get("path"))
                        && "ACTIVATE".equals(props.get("actionType"))
                        && props.get("eventTime") instanceof Long));
    }

    @Test
    void handleEvent_skipsWhenConnectorDisabled() {
        config.setEnableConnector(false);
        when(slingSettings.getRunModes()).thenReturn(Set.of("author"));
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        listener.handleEvent(createReplicationEvent("/content/wknd/en/page", ReplicationActionType.ACTIVATE));

        verifyNoInteractions(jobManager);
    }

    @Test
    void handleEvent_skipsPathOutsideRootPaths() {
        config.setRootPaths(new String[]{"/content/wknd"});
        when(slingSettings.getRunModes()).thenReturn(Set.of("author"));
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        listener.handleEvent(createReplicationEvent("/content/other/en/page", ReplicationActionType.ACTIVATE));

        verifyNoInteractions(jobManager);
    }

    @Test
    void handleEvent_skipsExcludedPath() {
        config.setExcludePaths(new String[]{"/content/wknd/private"});
        when(slingSettings.getRunModes()).thenReturn(Set.of("author"));
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        listener.handleEvent(createReplicationEvent("/content/wknd/private/page", ReplicationActionType.ACTIVATE));

        verifyNoInteractions(jobManager);
    }

    @Test
    void handleEvent_skipsTagReplication() {
        when(slingSettings.getRunModes()).thenReturn(Set.of("author"));
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        listener.handleEvent(createReplicationEvent("/content/cq:tags/wknd", ReplicationActionType.ACTIVATE));

        verifyNoInteractions(jobManager);
    }

    @Test
    void handleEvent_skipsUnsupportedPathPrefix() {
        when(slingSettings.getRunModes()).thenReturn(Set.of("author"));
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        listener.handleEvent(createReplicationEvent("/etc/designs/wknd", ReplicationActionType.ACTIVATE));

        verifyNoInteractions(jobManager);
    }

    @Test
    void handleEvent_acceptsDamPath() {
        when(slingSettings.getRunModes()).thenReturn(Set.of("author"));
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);
        when(jobManager.addJob(eq("searchstaxconnector/incremental-index"), anyMap())).thenReturn(job);

        listener.handleEvent(createReplicationEvent("/content/dam/wknd/image.jpg", ReplicationActionType.DELETE));

        verify(jobManager).addJob(eq("searchstaxconnector/incremental-index"), argThat(props ->
                "DELETE".equals(props.get("actionType"))));
    }
}