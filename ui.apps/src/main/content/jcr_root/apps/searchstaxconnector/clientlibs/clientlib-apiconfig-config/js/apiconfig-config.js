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
            var currentValue = el.value !== undefined ? el.value : "";
            var inner = el.querySelector("input:not([type='hidden']), textarea");
            if (inner && inner.value !== undefined) {
                currentValue = inner.value;
            }

            if (currentValue === normalized) {
                return;
            }

            if (el.value !== undefined) {
                el.value = normalized;
            }

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

    function triggerFieldValidation(el, isValid) {
        if (!el) { return; }
        var validationApi = $(el).adaptTo("foundation-validation");
        if (validationApi) {
            if (isValid) {
                if (typeof validationApi.clear === "function") {
                    validationApi.clear();
                }
            } else {
                if (typeof validationApi.checkValidity === "function") {
                    validationApi.checkValidity();
                }
                if (typeof validationApi.updateUI === "function") {
                    validationApi.updateUI();
                }
            }
        }
    }

    function validateApiConfigForm() {
        var isValid = true;
        var firstInvalidField = null;

        var fieldsToValidate = [
            { name: "endpointUrl", required: true, isUrl: true },
            { name: "selectEndpoint", required: true, isUrl: true },
            { name: "updateEndpoint", required: true, isUrl: true },
            { name: "apiToken", required: true, isSecret: true },
            { name: "selectToken", required: true, isSecret: true },
            { name: "updateToken", required: true, isSecret: true }
        ];

        fieldsToValidate.forEach(function (cfg) {
            var field = findField(cfg.name);
            if (!field) return;

            var val = "";
            var inner = field.querySelector("input:not([type='hidden']), textarea");
            if (inner) {
                val = (inner.value || "").trim();
            } else if (field.value !== undefined) {
                val = (field.value || "").trim();
            }

            var fieldValid = true;

            if (cfg.required) {
                if (cfg.isSecret) {
                    var hasSaved = false;
                    var placeholder = field.getAttribute("placeholder") || (inner && inner.getAttribute("placeholder"));
                    if (placeholder && placeholder.indexOf("Value saved") !== -1) {
                        hasSaved = true;
                    }
                    if (!val && !hasSaved) {
                        fieldValid = false;
                    }
                } else {
                    if (!val) {
                        fieldValid = false;
                    }
                }
            }

            if (fieldValid && cfg.isUrl && val) {
                try {
                    var parsed = new URL(val);
                    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
                        fieldValid = false;
                    }
                } catch (e) {
                    fieldValid = false;
                }
            }

            triggerFieldValidation(field, fieldValid);

            if (!fieldValid) {
                isValid = false;
                if (!firstInvalidField) {
                    firstInvalidField = field;
                }
            }
        });

        if (firstInvalidField) {
            var panel = firstInvalidField.closest("coral-panel");
            if (panel && !panel.selected) {
                panel.selected = true;
            }
        }

        return isValid;
    }

    function redirectAfterSave() {
        var href = "/aem/start.html";
        if (document.referrer && document.referrer.indexOf(window.location.host) !== -1) {
            href = document.referrer;
        }

        setTimeout(function () {
            window.location.href = href;
        }, 1500);
    }

    function init() {
        Coral.commons.ready(document, function () {
            loadConfiguration(0);

            var form = document.querySelector("form[action*='" + SAVE_PATH + "']");
            if (form) {
                form.addEventListener("submit", function (event) {
                    event.preventDefault();
                    event.stopImmediatePropagation();

                    if (validateApiConfigForm()) {
                        var $form = $(form);
                        var url = $form.attr("action");
                        var data = $form.serialize();

                        $.post(url, data)
                            .done(function () {
                                $(window).adaptTo("foundation-ui")
                                    .notify("Success", "API configuration saved successfully.", "success");
                                redirectAfterSave();
                            })
                            .fail(function (xhr) {
                                var message = "Unable to save configuration.";
                                try {
                                    var response = JSON.parse(xhr.responseText);
                                    if (response.message) {
                                        message = response.message;
                                    }
                                } catch (e) {
                                    console.error(LOG, "Failed parsing error response", e);
                                }
                                $(window).adaptTo("foundation-ui").alert("Validation Error", message, "error");
                            });
                    }
                }, true);
            }
        });
    }

    $(document).ready(init);

    document.addEventListener("foundation-contentloaded", function () {
        loadConfiguration(0);
    });

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
