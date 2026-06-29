package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexFailureNotificationServiceImplTest {

    @InjectMocks
    private IndexFailureNotificationServiceImpl notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailConfigService emailConfigService;

    @Test
    void sendFailureNotification_doesNothingWhenListIsNull() {
        notificationService.sendFailureNotification("batch-1", null);
        verifyNoInteractions(emailService);
    }

    @Test
    void sendFailureNotification_doesNothingWhenListIsEmpty() {
        notificationService.sendFailureNotification("batch-1", Collections.emptyList());
        verifyNoInteractions(emailService);
    }

    @Test
    void sendFailureNotification_doesNothingWhenNoRecipientsConfigured() {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[]{});

        notificationService.sendFailureNotification("batch-1", List.of(failedRequest()));

        verifyNoInteractions(emailService);
    }

    @Test
    void sendFailureNotification_sendsEmailWhenRecipientsConfigured() {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[]{"ops@example.com"});
        when(emailService.sendEmail(any(EmailRequest.class))).thenReturn(true);

        notificationService.sendFailureNotification("batch-42", List.of(failedRequest()));

        final ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).sendEmail(captor.capture());

        final EmailRequest emailRequest = captor.getValue();
        assertEquals("SearchStax Incremental Indexing Failure Notification", emailRequest.getSubject());
        assertEquals("ops@example.com", emailRequest.getRecipients()[0]);
        assertTrue(emailRequest.getBody().contains("batch-42"));
        assertTrue(emailRequest.getBody().contains("/content/site/page"));
    }

    @Test
    void sendFailureNotification_extractsJsonMessageFromResponseBody() {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[]{"ops@example.com"});
        when(emailService.sendEmail(any(EmailRequest.class))).thenReturn(true);

        final IndexRequest request = failedRequest();
        request.setResponseMessage("{\"message\":\"Document too large\"}");

        notificationService.sendFailureNotification("batch-99", List.of(request));

        final ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).sendEmail(captor.capture());
        assertTrue(captor.getValue().getBody().contains("Document too large"));
    }

    private static IndexRequest failedRequest() {
        final IndexRequest request = new IndexRequest(
                "/content/site/page",
                ReplicationActionType.ACTIVATE,
                System.currentTimeMillis());
        request.setStatusCode(500);
        request.setResponseMessage("Internal Server Error");
        return request;
    }
}
