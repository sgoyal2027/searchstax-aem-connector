(function (document, $) {
    "use strict";

    if (!window.location.pathname.includes("emailconfiguration")) {
        return;
    }

    var LOG = "[EmailConfig]";
    var LOAD_PATH = "/bin/searchstaxconnector/wizard/email-config-load";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/email-config-save";
    var TEST_PATH = "/bin/searchstaxconnector/wizard/email-config-test";
    var TEST_BUTTON_HINT = "Save all required email settings before sending a test email.";
    var TEST_BUTTON_DIRTY_HINT = "Save your changes before sending a test email.";
    var POPULATE_SETTLE_MS = 500;

    var textFields = [
        "smtpHost",
        "smtpUser",
        "fromEmail",
        "receiverEmails"
    ];

    var checkboxFields = [
        "smtpUseSSL",
        "smtpUseStartTLS",
        "notifyOnIndexingFailure"
    ];

    var savedConfig = null;
    var initialLoadInProgress = false;
    var formModifiedByUser = false;

    function getTestButton() {
        return document.querySelector("[data-granite-id='searchstax-email-test-button']")
            || document.getElementById("searchstax-email-test-button");
    }

    function readFieldValue(name) {
        var field = document.querySelector("[name='" + name + "']");
        if (!field) {
            return "";
        }

        if (field.value !== undefined && field.value !== null && String(field.value).trim() !== "") {
            return String(field.value).trim();
        }

        var inner = field.querySelector("input:not([type='hidden']), textarea");
        return inner ? String(inner.value || "").trim() : "";
    }

    function readCheckboxValue(name) {
        var field = document.querySelector("[name='" + name + "']");
        if (!field) {
            return false;
        }

        if (field.checked !== undefined) {
            return Boolean(field.checked);
        }

        var inner = field.querySelector("input[type='checkbox']");
        return inner ? Boolean(inner.checked) : false;
    }

    function captureSavedConfig(data) {
        savedConfig = {
            smtpHost: data.smtpHost || "",
            smtpPort: data.smtpPort || "",
            smtpUser: data.smtpUser || "",
            fromEmail: data.fromEmail || "",
            receiverEmails: data.receiverEmails || "",
            smtpUseSSL: Boolean(data.smtpUseSSL),
            smtpUseStartTLS: Boolean(data.smtpUseStartTLS),
            notifyOnIndexingFailure: Boolean(data.notifyOnIndexingFailure),
            hasSavedPassword: Boolean(data.hasSavedPassword),
            configurationSaved: Boolean(data.configurationSaved)
        };
    }

    function hasUnsavedPasswordEntry() {
        return Boolean(readFieldValue("smtpPassword"));
    }

    function isTestEmailAllowed() {
        if (!savedConfig || !savedConfig.configurationSaved) {
            return false;
        }

        return !formModifiedByUser && !hasUnsavedPasswordEntry();
    }

    function setButtonDisabled(button, disabled) {
        Coral.commons.ready(button, function (el) {
            el.disabled = disabled;
        });
        button.disabled = disabled;
    }

    function updateTestButtonState() {
        var button = getTestButton();
        if (!button) {
            return;
        }

        var allowed = isTestEmailAllowed();
        setButtonDisabled(button, !allowed);

        if (!allowed) {
            var hint = TEST_BUTTON_HINT;
            if (formModifiedByUser || hasUnsavedPasswordEntry()) {
                hint = TEST_BUTTON_DIRTY_HINT;
            } else if (savedConfig && !savedConfig.configurationSaved) {
                hint = TEST_BUTTON_HINT;
            }
            button.setAttribute("title", hint);
        } else {
            button.removeAttribute("title");
        }
    }

    function buildTestPayload() {
        var params = new URLSearchParams();

        textFields.forEach(function (fieldName) {
            params.append(fieldName, readFieldValue(fieldName));
        });

        params.append("smtpPort", readFieldValue("smtpPort"));

        checkboxFields.forEach(function (fieldName) {
            params.append(fieldName, readCheckboxValue(fieldName) ? "true" : "false");
        });

        var password = readFieldValue("smtpPassword");
        if (password) {
            params.append("smtpPassword", password);
        }

        return params;
    }

    function setFieldValue(name, value) {
        var field = document.querySelector("[name='" + name + "']");
        if (!field) {
            return;
        }

        Coral.commons.ready(field, function (el) {
            if (el.value !== undefined) {
                el.value = value === undefined || value === null ? "" : value;
            }

            var inner = el.querySelector("input:not([type='hidden']), textarea");
            if (inner) {
                inner.value = value === undefined || value === null ? "" : String(value);
            }
        });
    }

    function setCheckboxValue(name, checked) {
        var field = document.querySelector("[name='" + name + "']");
        if (!field) {
            return;
        }

        Coral.commons.ready(field, function (el) {
            if (el.checked !== undefined) {
                el.checked = Boolean(checked);
            }
        });
    }

    function finishInitialPopulation() {
        initialLoadInProgress = false;
        formModifiedByUser = false;
        updateTestButtonState();
    }

    function populateForm(data) {
        initialLoadInProgress = true;
        formModifiedByUser = false;
        captureSavedConfig(data);

        textFields.forEach(function (fieldName) {
            setFieldValue(fieldName, data[fieldName]);
        });

        setFieldValue("smtpPort", data.smtpPort || "");

        checkboxFields.forEach(function (fieldName) {
            setCheckboxValue(fieldName, Boolean(data[fieldName]));
        });

        setTimeout(finishInitialPopulation, POPULATE_SETTLE_MS);
    }

    function loadConfiguration() {
        $.getJSON(LOAD_PATH, populateForm).fail(function (xhr) {
            console.error(LOG, "Failed loading configuration", xhr);
            savedConfig = null;
            initialLoadInProgress = false;
            formModifiedByUser = false;
            updateTestButtonState();
        });
    }

    function onFormChangedByUser() {
        if (initialLoadInProgress) {
            return;
        }

        formModifiedByUser = true;
        updateTestButtonState();
    }

    function bindFormChangeTracking() {
        var form = document.querySelector("form[action*='email-config-save']");
        if (!form || form.dataset.searchstaxEmailChangeBound === "true") {
            return;
        }

        form.dataset.searchstaxEmailChangeBound = "true";
        form.addEventListener("input", onFormChangedByUser, true);
        form.addEventListener("change", onFormChangedByUser, true);
    }

    function bindTestEmailButton() {
        var button = getTestButton();
        if (!button || button.dataset.searchstaxEmailTestBound === "true") {
            return;
        }

        button.dataset.searchstaxEmailTestBound = "true";

        button.addEventListener("click", function (event) {
            event.preventDefault();
            if (!isTestEmailAllowed()) {
                setTestResult(
                    formModifiedByUser || hasUnsavedPasswordEntry()
                        ? TEST_BUTTON_DIRTY_HINT
                        : TEST_BUTTON_HINT,
                    true
                );
                return;
            }
            sendTestEmail(button);
        });
    }

    function getCsrfToken() {
        return fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" })
            .then(function (response) {
                return response.ok ? response.json() : null;
            })
            .then(function (data) {
                return data && data.token ? data.token : "";
            })
            .catch(function () {
                return "";
            });
    }

    function setTestResult(message, isError) {
        var result = document.querySelector("[data-granite-id='searchstax-email-test-result']")
            || document.getElementById("searchstax-email-test-result");
        if (!result) {
            return;
        }
        result.textContent = message || "";
        result.style.color = isError ? "#c9252d" : "#12805c";
        result.style.marginTop = "8px";
    }

    function sendTestEmail(button) {
        setButtonDisabled(button, true);
        setTestResult("Sending test email using saved SMTP configuration...", false);

        getCsrfToken().then(function (csrfToken) {
            var headers = {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            };
            if (csrfToken) {
                headers["CSRF-Token"] = csrfToken;
            }

            return fetch(TEST_PATH, {
                method: "POST",
                headers: headers,
                body: buildTestPayload().toString(),
                credentials: "same-origin"
            });
        }).then(function (response) {
            return response.text().then(function (text) {
                var data = {};
                try {
                    data = JSON.parse(text);
                } catch (error) {
                    data = { success: false, message: "Unexpected response from server." };
                }
                return { ok: response.ok, data: data };
            });
        }).then(function (result) {
            var success = result.ok && Boolean(result.data.success);
            setTestResult(
                result.data.message || (success ? "Test email sent." : "Test email failed."),
                !success
            );
        }).catch(function (error) {
            setTestResult("Unable to reach test email endpoint: " + error.message, true);
        }).finally(function () {
            updateTestButtonState();
        });
    }

    function refreshSavedConfigAfterSave() {
        initialLoadInProgress = true;
        formModifiedByUser = false;

        $.getJSON(LOAD_PATH, function (data) {
            captureSavedConfig(data);
            setFieldValue("smtpPassword", "");
            setTimeout(finishInitialPopulation, POPULATE_SETTLE_MS);
        });
    }

    function isSaveRequest(requestUrl) {
        return requestUrl && requestUrl.split("?")[0].endsWith(SAVE_PATH);
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
        updateTestButtonState();

        Coral.commons.ready(document, function () {
            loadConfiguration();
            bindFormChangeTracking();
            bindTestEmailButton();
        });

        $(document).ajaxSuccess(function (event, xhr, settings) {
            if (!isSaveRequest(settings.url)) {
                return;
            }

            refreshSavedConfigAfterSave();

            $(window).adaptTo("foundation-ui")
                .notify("Success", "Email configuration saved successfully.", "success");
            redirectAfterSave();
        });

        $(document).ajaxError(function (event, xhr, settings) {
            if (!isSaveRequest(settings.url)) {
                return;
            }

            var message = "Unable to save email configuration.";
            try {
                var response = JSON.parse(xhr.responseText);
                if (response.message) {
                    message = response.message;
                }
            } catch (e) {
                console.error(LOG, "Failed parsing error response", e);
            }

            $(window).adaptTo("foundation-ui").alert("Save Failed", message, "error");
            updateTestButtonState();
        });
    }

    $(document).ready(init);

    document.addEventListener("foundation-contentloaded", function () {
        bindFormChangeTracking();
        bindTestEmailButton();
        loadConfiguration();
    });

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
