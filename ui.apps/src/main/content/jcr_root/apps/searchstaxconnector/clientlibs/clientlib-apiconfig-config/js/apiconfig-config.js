(function (document, $) {
    "use strict";

    if (!window.location.pathname.includes("apiconfig")) {
        return;
    }

    var LOG = "[ApiConfig]";
    var LOAD_PATH = "/bin/searchstaxconnector/wizard/api-config-load";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/api-config-save";

    var textFields = [
        "endpointUrl",
        "selectEndpoint",
        "updateEndpoint",
        "autoSuggestApi",
        "relatedSearchesEndpoint",
        "popularSearchesEndpoint",
        "analyticsTrackingUrl",
        "analyticsReportingUrl",
        "forwardGeocodingEndpoint",
        "reverseGeocodingEndpoint"
    ];

    var secretFields = [
        "apiToken",
        "selectToken",
        "updateToken",
        "discoveryApiKey",
        "analyticsTrackingKey",
        "analyticsReportingApiKey"
    ];

    function findField(name) {
        if (window.SearchStaxConfigUtil && window.SearchStaxConfigUtil.findNamedField) {
            return window.SearchStaxConfigUtil.findNamedField(name);
        }

        return document.querySelector("[name='" + name + "']");
    }

    function setFieldValue(name, value) {
        var field = findField(name);
        if (!field) {
            console.warn(LOG, "Field not found:", name);
            return;
        }

        var normalized = value === undefined || value === null ? "" : String(value);

        Coral.commons.ready(field, function (el) {
            if (el.value !== undefined) {
                el.value = normalized;
            }

            var inner = el.querySelector("input:not([type='hidden']), textarea");
            if (inner) {
                inner.value = normalized;
            }

            el.dispatchEvent(new Event("change", { bubbles: true }));
        });
    }

    function populateForm(data) {
        if (!data) {
            return false;
        }

        var endpointField = findField("endpointUrl");
        if (!endpointField) {
            return false;
        }

        textFields.forEach(function (fieldName) {
            setFieldValue(fieldName, data[fieldName]);
        });

        if (window.SearchStaxConfigUtil && window.SearchStaxConfigUtil.applySecretFieldStates) {
            window.SearchStaxConfigUtil.applySecretFieldStates(data, secretFields, 0);
        }

        return true;
    }

    function loadConfiguration(attempt) {
        attempt = attempt || 0;

        $.getJSON(LOAD_PATH, function (data) {
            if (!populateForm(data) && attempt < 10) {
                setTimeout(function () {
                    loadConfiguration(attempt + 1);
                }, 200);
            }
        }).fail(function (xhr) {
            console.error(LOG, "Failed loading configuration", xhr);
        });
    }

    function init() {
        Coral.commons.ready(document, function () {
            loadConfiguration(0);
        });

        if (window.SearchStaxConfigUtil && window.SearchStaxConfigUtil.attachSaveHandlers) {
            window.SearchStaxConfigUtil.attachSaveHandlers(
                SAVE_PATH,
                "API configuration saved successfully."
            );

            $(document).ajaxSuccess(function (event, xhr, settings) {
                if (window.SearchStaxConfigUtil.isServletRequest(settings.url, SAVE_PATH)) {
                    loadConfiguration(0);
                }
            });
        }
    }

    $(document).ready(init);

    document.addEventListener("foundation-contentloaded", function () {
        loadConfiguration(0);
    });

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
