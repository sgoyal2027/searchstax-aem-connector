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
import org.osgi.service.event.Event;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
@ExtendWith(MockitoExtension.class)
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
}