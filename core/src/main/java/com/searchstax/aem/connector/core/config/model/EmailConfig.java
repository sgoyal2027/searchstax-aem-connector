package com.searchstax.aem.connector.core.config.model;

public class EmailConfig {

    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String fromEmail;
    private String receiverEmails;
    private boolean smtpUseSsl;
    private boolean smtpUseStartTls;
    private boolean smtpRequireStartTls;
    private boolean debugEmail;
    private boolean oauthFlow;
    private boolean notifyOnIndexingFailure;

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(final String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(final int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(final String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(final String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(final String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getReceiverEmails() {
        return receiverEmails;
    }

    public void setReceiverEmails(final String receiverEmails) {
        this.receiverEmails = receiverEmails;
    }

    public boolean isSmtpUseSsl() {
        return smtpUseSsl;
    }

    public void setSmtpUseSsl(final boolean smtpUseSsl) {
        this.smtpUseSsl = smtpUseSsl;
    }

    public boolean isSmtpUseStartTls() {
        return smtpUseStartTls;
    }

    public void setSmtpUseStartTls(final boolean smtpUseStartTls) {
        this.smtpUseStartTls = smtpUseStartTls;
    }

    public boolean isSmtpRequireStartTls() {
        return smtpRequireStartTls;
    }

    public void setSmtpRequireStartTls(final boolean smtpRequireStartTls) {
        this.smtpRequireStartTls = smtpRequireStartTls;
    }

    public boolean isDebugEmail() {
        return debugEmail;
    }

    public void setDebugEmail(final boolean debugEmail) {
        this.debugEmail = debugEmail;
    }

    public boolean isOauthFlow() {
        return oauthFlow;
    }

    public void setOauthFlow(final boolean oauthFlow) {
        this.oauthFlow = oauthFlow;
    }

    public boolean isNotifyOnIndexingFailure() {
        return notifyOnIndexingFailure;
    }

    public void setNotifyOnIndexingFailure(final boolean notifyOnIndexingFailure) {
        this.notifyOnIndexingFailure = notifyOnIndexingFailure;
    }
}
