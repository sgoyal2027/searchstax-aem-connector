package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementalQueueServiceImplTest {

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource pendingRoot;

    @Mock
    private Resource queueItemOne;

    @Mock
    private Resource queueItemTwo;

    @InjectMocks
    private IncrementalQueueServiceImpl queueService;

    @Test
    void nodeNameForPath_sanitizesSpecialCharacters() {
        final String nodeName =
                IncrementalQueueServiceImpl.nodeNameForPath("/content/wknd/en/my page");

        assertFalse(nodeName.contains(" "));
        assertFalse(nodeName.contains("/"));
    }

    @Test
    void nodeNameForPath_truncatesVeryLongPaths() {
        final String longPath = "/content/" + "a".repeat(250);
        final String nodeName = IncrementalQueueServiceImpl.nodeNameForPath(longPath);

        assertTrue(nodeName.length() <= 200);
    }

    @Test
    void clearPendingQueue_removesAllPendingItems() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(IncrementalQueueServiceImpl.PENDING_QUEUE_PATH)).thenReturn(pendingRoot);
        when(pendingRoot.getChildren()).thenReturn(List.of(queueItemOne, queueItemTwo));

        final int removed = queueService.clearPendingQueue();

        assertEquals(2, removed);
        verify(resourceResolver).delete(queueItemOne);
        verify(resourceResolver).delete(queueItemTwo);
        verify(resourceResolver).commit();
    }

    @Test
    void clearPendingQueue_returnsZeroWhenQueueMissing() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(IncrementalQueueServiceImpl.PENDING_QUEUE_PATH)).thenReturn(null);

        assertEquals(0, queueService.clearPendingQueue());
    }
}
