(function (document, $) {
    "use strict";

    if (!window.location.pathname.includes("initialsetup")) {
        return;
    }

    var LOG = "[InitialSetup]";
    var LOAD_PATH = "/bin/searchstaxconnector/wizard/initial-setup-load";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/initial-setup-config";
    var ROOT_PATHS_ID = "searchstax-initial-root-paths-multifield";
    var EXCLUDE_PATHS_ID = "searchstax-initial-exclude-paths-multifield";
    var MAX_LOAD_ATTEMPTS = 25;

    function util() {
        return window.SearchStaxMultifieldUtil;
    }

    function populateForm(data, done) {
        var mf = util();
        if (!mf) {
            console.warn(LOG, "SearchStaxMultifieldUtil is not loaded");
            if (done) {
                done(false);
            }
            return;
        }

        var rootMultifield = mf.findMultifield({ id: ROOT_PATHS_ID, fieldName: "rootPaths", index: 0 });
        var excludeMultifield = mf.findMultifield({ id: EXCLUDE_PATHS_ID, fieldName: "excludePaths", index: 1 });
        var pending = 0;
        var success = Boolean(rootMultifield);

        if (!rootMultifield) {
            console.warn(LOG, "Root paths multifield not found yet");
        }

        mf.setCheckboxValue(document, "enableConnector", data.enableConnector);

        function checkDone() {
            pending -= 1;
            if (pending <= 0 && done) {
                done(success);
            }
        }

        if (data.rootPaths && data.rootPaths.length > 0 && rootMultifield) {
            pending += 1;
            mf.populatePathMultifield(rootMultifield, "rootPaths", data.rootPaths, function (ok) {
                if (!ok) {
                    success = false;
                }
                checkDone();
            });
        }

        if (data.excludePaths && data.excludePaths.length > 0 && excludeMultifield) {
            pending += 1;
            mf.populatePathMultifield(excludeMultifield, "excludePaths", data.excludePaths, function (ok) {
                if (!ok) {
                    success = false;
                }
                checkDone();
            });
        } else if (data.excludePaths && data.excludePaths.length > 0 && !excludeMultifield) {
            success = false;
        }

        if (data.allowedFiles && data.allowedFiles.length > 0) {
            var allowedFilesField = mf.findNamedField(document, "allowedFiles");
            if (allowedFilesField) {
                Coral.commons.ready(allowedFilesField, function (select) {
                    select.values = data.allowedFiles;
                });
            }
        }

        if (pending === 0 && done) {
            done(success);
        }
    }

    function loadConfiguration(attempt) {
        attempt = attempt || 0;

        $.getJSON(LOAD_PATH, function (data) {
            populateForm(data, function (success) {
                if (!success && attempt < MAX_LOAD_ATTEMPTS) {
                    setTimeout(function () {
                        loadConfiguration(attempt + 1);
                    }, 300);
                }
            });
        }).fail(function (xhr) {
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
        Coral.commons.ready(document, function () {
            loadConfiguration(0);
        });

        $(document).ajaxSuccess(function (event, xhr, settings) {
            if (!isSaveRequest(settings.url)) {
                return;
            }

            $(window).adaptTo("foundation-ui")
                .notify("Success", "Initial configuration saved successfully.", "success");
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

    document.addEventListener("foundation-contentloaded", function () {
        loadConfiguration(0);
    });

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
