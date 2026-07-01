(function (document, $) {
    "use strict";

    if (!window.location.pathname.includes("indexingconfig")) {
        return;
    }
    
    var LOG = "[FullIndex]";
    var LOAD_PATH = "/bin/searchstaxconnector/wizard/full-index-load";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/indexing-config-save";
    var ROOT_PATHS_ID = "searchstax-initial-root-paths-multifield";
    var INCLUDE_PATHS_ID = "searchstax-include-paths-multifield";
    var EXCLUDE_PATHS_ID = "searchstax-fullindex-exclude-paths-multifield";
    var MAX_LOAD_ATTEMPTS = 25;

    function util() {
        return window.SearchStaxMultifieldUtil;
    }

    function getIncludePathsMultifield() {
        return util().findMultifield({ id: INCLUDE_PATHS_ID, fieldName: "path", index: 0 });
    }

    function serializeIncludePaths() {
        var multifield = getIncludePathsMultifield();
        var hiddenField = document.querySelector("[name='./includePathsJson']");
        var includePaths = [];
        var mf = util();

        if (!multifield || !hiddenField || !mf) {
            return;
        }

        multifield.items.getAll().forEach(function (item) {
            var pathField = mf.findNamedField(item, "path");
            var childCheckbox = mf.findNamedField(item, "includeChildPath");
            var pathValue = "";

            if (pathField) {
                pathValue = pathField.value || "";
                if (!pathValue) {
                    var inner = pathField.querySelector("input");
                    pathValue = inner ? inner.value : "";
                }
            }

            if (!String(pathValue).trim()) {
                return;
            }

            includePaths.push({
                path: String(pathValue).trim(),
                includeChildPath: childCheckbox ? Boolean(childCheckbox.checked) : false
            });
        });

        hiddenField.value = JSON.stringify(includePaths);
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

        var rootMultifield = mf.findMultifield({
            id: ROOT_PATHS_ID,
            fieldName: "rootPaths",
            index: 0
        });

        var includeMultifield = getIncludePathsMultifield();
        var excludeMultifield = mf.findMultifield({ id: EXCLUDE_PATHS_ID, fieldName: "excludePaths", index: 1 });
        var pending = 0;
        var success = Boolean(rootMultifield);

        if (!rootMultifield) {
            console.warn(LOG, "Root paths multifield not found yet");
            success = false;
        }

        mf.setCheckboxValue(document, "enableConnector", data.enableConnector);

        if (data.rootPaths && data.rootPaths.length > 0 && rootMultifield) {
            pending += 1;

            mf.populatePathMultifield(
                rootMultifield,
                "rootPaths",
                data.rootPaths,
                function (ok) {
                    if (!ok) {
                        success = false;
                    }
                    checkDone();
                });
        }

        var allowedFilesField =
            mf.findNamedField(document, "allowedFiles");

        if (allowedFilesField) {

            Coral.commons.ready(
                allowedFilesField,
                function (select) {

                    select.values = data.allowedFiles || [];
                });
        }

        function checkDone() {
            pending -= 1;
            if (pending <= 0 && done) {
                done(success);
            }
        }

        if (data.includePaths && data.includePaths.length > 0) {
            if (!includeMultifield) {
                success = false;
            } else {
                pending += 1;
                mf.populateIncludePaths(includeMultifield, data.includePaths, function (ok) {
                    if (!ok) {
                        success = false;
                    }
                    checkDone();
                });
            }
        }

        if (data.excludePaths && data.excludePaths.length > 0) {
            if (!excludeMultifield) {
                console.warn(LOG, "Exclude paths multifield not found yet");
                success = false;
            } else {
                pending += 1;
                mf.populatePathMultifield(excludeMultifield, "excludePaths", data.excludePaths, function (ok) {
                    if (!ok) {
                        success = false;
                    }
                    checkDone();
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

    function bindSaveSerialization() {
        var form = document.querySelector("form[action*='indexing-config-save']");
        if (!form || form.dataset.searchstaxIncludePathsBound === "true") {
            return;
        }

        form.dataset.searchstaxIncludePathsBound = "true";
        form.addEventListener("submit", function () {
            serializeIncludePaths();
        }, true);

        $(document).on("click", ".foundation-wizard-control[type='submit']", function () {
            serializeIncludePaths();
        });
    }

    function isSaveRequest(requestUrl) {
        return requestUrl && requestUrl.indexOf(SAVE_PATH) !== -1;
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
            bindSaveSerialization();
        });

        $(document).ajaxSuccess(function (event, xhr, settings) {
            if (!isSaveRequest(settings.url)) {
                return;
            }

            $(window).adaptTo("foundation-ui")
                .notify("Success", "Indexing configuration saved successfully.", "success");
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
        bindSaveSerialization();
        loadConfiguration(0);
    });

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
