(function (document, $) {
    "use strict";

    if (!window.location.pathname.includes("initialsetup")) {
        return;
    }

    var LOG = "[InitialSetup]";
    var LOAD_PATH = "/bin/searchstaxconnector/wizard/initial-setup-load";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/initial-setup-config";

    function findMultifieldContainingField(fieldName) {
        var multifields = document.querySelectorAll("coral-multifield");
        var i;

        for (i = 0; i < multifields.length; i++) {
            if (multifields[i].querySelector("[name='./" + fieldName + "']")) {
                return multifields[i];
            }
        }

        return null;
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

    function populateMultifield(fieldName, values) {
        var multifield = findMultifieldContainingField(fieldName);
        if (!multifield) {
            console.warn(LOG, "Multifield not found:", fieldName);
            return;
        }

        values.forEach(function (value, index) {
            multifield.items.add();

            setTimeout(function () {
                var items = multifield.items.getAll();
                var item = items[index];
                var field;

                if (!item) {
                    return;
                }

                field = item.querySelector("[name='./" + fieldName + "']");
                if (!field) {
                    return;
                }

                Coral.commons.ready(field, function (el) {
                    if (el.value !== undefined) {
                        el.value = value;
                    }
                });
            }, 200);
        });
    }

    function populateForm(data) {
        setCheckboxValue("./enableConnector", data.enableConnector);

        if (data.rootPaths && data.rootPaths.length > 0) {
            populateMultifield("rootPaths", data.rootPaths);
        }

        if (data.excludePaths && data.excludePaths.length > 0) {
            populateMultifield("excludePaths", data.excludePaths);
        }

        if (data.allowedFiles && data.allowedFiles.length > 0) {
            var allowedFilesField = document.querySelector("[name='./allowedFiles']");
            if (allowedFilesField) {
                Coral.commons.ready(allowedFilesField, function (select) {
                    select.values = data.allowedFiles;
                });
            }
        }

        setCheckboxValue("./maintenanceModeManual", data.maintenanceModeManual);
        setFieldValue("./maintenanceMessage", data.maintenanceMessage || "");
        setFieldValue("./maintenanceFailureThreshold", data.maintenanceFailureThreshold || 3);
    }

    function loadConfiguration() {
        $.getJSON(LOAD_PATH, populateForm).fail(function (xhr) {
            console.error(LOG, "Failed loading configuration", xhr);
        });
    }

    function isSaveRequest(requestUrl) {
        return requestUrl && requestUrl.split("?")[0].indexOf(SAVE_PATH) !== -1;
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
        Coral.commons.ready(document, loadConfiguration);

        $(document).ajaxSuccess(function (event, xhr, settings) {
            if (!isSaveRequest(settings.url)) {
                return;
            }

            var ui = $(window).adaptTo("foundation-ui");
            ui.notify("Success", "Initial configuration saved successfully.", "success");
            redirectAfterSave();
        });

        $(document).ajaxError(function (event, xhr, settings) {
            if (!isSaveRequest(settings.url)) {
                return;
            }

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

    $(document).ready(init);

    document.addEventListener("foundation-contentloaded", loadConfiguration);

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
