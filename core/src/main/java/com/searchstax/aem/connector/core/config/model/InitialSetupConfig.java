package com.searchstax.aem.connector.core.config.model;

import java.util.Arrays;

public class InitialSetupConfig {

    private boolean enableConnector;
    private String[] rootPaths;
    private String[] excludePaths;
    private String[] allowedFiles;
    private boolean maintenanceModeManual;
    private String maintenanceMessage;
    private int maintenanceFailureThreshold;

    public boolean isEnableConnector() {
        return enableConnector;
    }

    public void setEnableConnector(boolean enableConnector) {
        this.enableConnector = enableConnector;
    }

    public String[] getRootPaths() {
        return rootPaths != null
                ? Arrays.copyOf(
                rootPaths,
                rootPaths.length)
                : null;
    }

    public void setRootPaths(String[] rootPaths) {
        this.rootPaths =
                rootPaths != null 
                        ? Arrays.copyOf(
                        rootPaths,
                        rootPaths.length)
                        : null;
    }

    public String[] getExcludePaths() {

        return excludePaths != null
                ? Arrays.copyOf(
                excludePaths,
                excludePaths.length)
                : null;
    }

    public void setExcludePaths(String[] excludePaths) {

        this.excludePaths =
                excludePaths != null
                        ? Arrays.copyOf(
                        excludePaths,
                        excludePaths.length)
                        : null;
    }

    public String[] getAllowedFiles() {

        return allowedFiles != null
                ? Arrays.copyOf(
                allowedFiles,
                allowedFiles.length)
                : null;
    }

    public void setAllowedFiles(String[] allowedFiles) {
        this.allowedFiles =
                allowedFiles != null
                        ? Arrays.copyOf(
                        allowedFiles,
                        allowedFiles.length)
                        : null;
    }

    public boolean isMaintenanceModeManual() {
        return maintenanceModeManual;
    }

    public void setMaintenanceModeManual(final boolean maintenanceModeManual) {
        this.maintenanceModeManual = maintenanceModeManual;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public void setMaintenanceMessage(final String maintenanceMessage) {
        this.maintenanceMessage = maintenanceMessage;
    }

    public int getMaintenanceFailureThreshold() {
        return maintenanceFailureThreshold;
    }

    public void setMaintenanceFailureThreshold(final int maintenanceFailureThreshold) {
        this.maintenanceFailureThreshold = maintenanceFailureThreshold;
    }
}