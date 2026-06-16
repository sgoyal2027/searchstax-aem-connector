package com.searchstax.aem.connector.core.dto.request;

import java.util.Arrays;

public class EmailRequest {

    private String subject;
    private String body;
    private String[] recipients;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String[] getRecipients() {

        return recipients != null
                ? Arrays.copyOf(
                recipients,
                recipients.length)
                : null;
    }

    public void setRecipients(String[] recipients) {

        this.recipients =
                recipients != null
                        ? Arrays.copyOf(
                        recipients,
                        recipients.length)
                        : null;
    }

}
