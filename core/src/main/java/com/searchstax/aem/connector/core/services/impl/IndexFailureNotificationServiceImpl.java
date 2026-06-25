package com.searchstax.aem.connector.core.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.services.IndexFailureNotificationService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component(service = IndexFailureNotificationService.class)
public class IndexFailureNotificationServiceImpl
        implements IndexFailureNotificationService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    IndexFailureNotificationServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    @Reference
    private EmailService emailService;

    @Reference
    private EmailConfigService emailConfigService;

    @Override
    public void sendFailureNotification(
            String batchId,
            List<IndexRequest> failedRequests) {

        if (failedRequests == null
                || failedRequests.isEmpty()) {

        LOG.info(
                "No failed requests found for BatchId={}",
                batchId);

        return;
        }

        try {

            String[] recipients =
                    emailConfigService
                            .getReceiverAddresses();

            if (recipients.length == 0) {

                LOG.warn(
                        "No recipients configured for failure notification.");

                return;
            }

            String subject =
                    "SearchStax Incremental Indexing Failure Notification";

            StringBuilder body =
                    new StringBuilder();

            body.append("<html><body>");

            body.append("<h2>SearchStax Incremental Index Failure Notification</h2>");

            body.append("<p><b>Batch ID:</b> ")
                    .append(batchId)
                    .append("</p>");

            body.append("<p><b>Failed Requests:</b> ")
                    .append(failedRequests.size())
                    .append("</p>");

            body.append("<hr/>");

            body.append("<ul>");

            for (IndexRequest request : failedRequests) {

                body.append("<div style='margin-bottom:20px;'>");

                body.append("<b>Path:</b> ")
                        .append(request.getPath())
                        .append("<br/>");

                body.append("<b>Status Code:</b> ")
                        .append(request.getStatusCode())
                        .append("<br/>");

                body.append("<b>Response Message:</b> ")
                        .append(formatResponseMessage(
                                request.getResponseMessage()))
                        .append("<br/>");

                body.append("</div>");
            }

            body.append("</ul>");
            body.append("</body></html>");

            EmailRequest emailRequest =
                    new EmailRequest();

            emailRequest.setSubject(
                    subject);

            emailRequest.setBody(
                    body.toString());

            emailRequest.setRecipients(
                    recipients);

            boolean sent =
                    emailService.sendEmail(
                            emailRequest);

            if (sent) {

                LOG.info(
                        "Failure notification email sent successfully. BatchId={}",
                        batchId);

            } else {

                LOG.error(
                        "Failure notification email failed. BatchId={}",
                        batchId);
            }

        } catch (Exception e) {

            LOG.error(
                    "Error while sending failure notification. BatchId={}",
                    batchId,
                    e);
        }
    }

    private String formatResponseMessage(
            String responseMessage) {

        if (responseMessage == null
                || responseMessage.isBlank()) {
            return "N/A";
        }
        try {
            JsonNode jsonNode =
                    OBJECT_MAPPER.readTree(responseMessage);
            if (jsonNode.has("message")) {
                return jsonNode.get("message").asText();
            }
        } catch (Exception e) {
            LOG.debug(
                    "Unable to parse response message as JSON. Returning original response.");
            return responseMessage;
        }
        return responseMessage;
    }
}
