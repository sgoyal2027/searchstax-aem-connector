package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.SlingHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class JsonServletResponseUtil {

    private JsonServletResponseUtil() {
    }

    public static void writeSuccess(
            final SlingHttpServletResponse response,
            final String message) throws IOException {

        writeJson(
                response,
                HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"" + escapeJson(message) + "\"}");
    }

    public static void writeError(
            final SlingHttpServletResponse response,
            final int status,
            final String message) throws IOException {

        writeJson(
                response,
                status,
                "{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}");
    }

    public static void writeBadRequest(
            final SlingHttpServletResponse response,
            final String message) throws IOException {

        writeError(response, HttpServletResponse.SC_BAD_REQUEST, message);
    }

    public static void writeInternalError(
            final SlingHttpServletResponse response,
            final String message) throws IOException {

        writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    private static void writeJson(
            final SlingHttpServletResponse response,
            final int status,
            final String json) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    static String escapeJson(final String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");
    }
}
