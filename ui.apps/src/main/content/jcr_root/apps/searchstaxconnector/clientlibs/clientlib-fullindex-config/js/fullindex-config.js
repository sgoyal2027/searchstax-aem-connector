(function (document, $) {
    "use strict";

    if (!window.location.pathname.includes("fullindexing")) {
        return;
    }

    var LOG = "[FullIndex]";
    var LOAD_PATH = "/bin/searchstaxconnector/wizard/full-index-load";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/fullindex-config-save";

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

    function getIncludePathsMultifield() {
        return document.getElementById("searchstax-include-paths-multifield")
            || findMultifieldContainingField("path");
    }

    function serializeIncludePaths() {
        var multifield = getIncludePathsMultifield();
        var hiddenField = document.querySelector("[name='./includePathsJson']");
        var includePaths = [];

        if (!multifield || !hiddenField) {
            return;
        }

        multifield.items.getAll().forEach(function (item) {
            var pathField = item.querySelector("[name='./path']");
            var childCheckbox = item.querySelector("[name='./includeChildPath']");
            var pathValue = "";

            if (pathField) {
                pathValue = pathField.value || "";
            }

            if (!pathValue.trim()) {
                return;
            }

            includePaths.push({
                path: pathValue,
                includeChildPath: childCheckbox ? Boolean(childCheckbox.checked) : false
            });
        });

        hiddenField.value = JSON.stringify(includePaths);
    }

    function populateIncludePaths(includePaths) {
        var multifield = getIncludePathsMultifield();
        if (!multifield) {
            console.warn(LOG, "Include Paths multifield not found");
            return;
        }

        includePaths.forEach(function (includePath, index) {
            multifield.items.add();

            setTimeout(function () {
                var items = multifield.items.getAll();
                var item = items[index];
                var pathField;
                var checkbox;

                if (!item) {
                    return;
                }

                pathField = item.querySelector("[name='./path']");
                if (pathField) {
                    Coral.commons.ready(pathField, function (el) {
                        if (el.value !== undefined) {
                            el.value = includePath.path;
                        }
                    });
                }

                checkbox = item.querySelector("[name='./includeChildPath']");
                if (checkbox) {
                    Coral.commons.ready(checkbox, function (el) {
                        if (el.checked !== undefined) {
                            el.checked = Boolean(includePath.includeChildPath);
                        }
                    });
                }
            }, 200);
        });
    }

    function populateExcludePaths(values) {
        var multifield = findMultifieldContainingField("excludePaths");
        if (!multifield) {
            console.warn(LOG, "Exclude Paths multifield not found");
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

                field = item.querySelector("[name='./excludePaths']");
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
        var rootPathField = document.querySelector("[name='./rootPath']");

        if (rootPathField && data.rootPath) {
            Coral.commons.ready(rootPathField, function (el) {
                if (el.value !== undefined) {
                    el.value = data.rootPath;
                }
            });
        }

        if (data.includePaths && data.includePaths.length > 0) {
            populateIncludePaths(data.includePaths);
        }

        if (data.excludePaths && data.excludePaths.length > 0) {
            populateExcludePaths(data.excludePaths);
        }
    }

    function loadConfiguration() {
        $.getJSON(LOAD_PATH, populateForm).fail(function (xhr) {
            console.error(LOG, "Failed loading configuration", xhr);
        });
    }

    function bindSaveSerialization() {
        var form = document.querySelector("form[action*='fullindex-config-save']");
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
            loadConfiguration();
            bindSaveSerialization();
        });

        $(document).ajaxSuccess(function (event, xhr, settings) {
            if (!isSaveRequest(settings.url)) {
                return;
            }

            $(window).adaptTo("foundation-ui")
                .notify("Success", "Full Index configuration saved successfully.", "success");
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
        loadConfiguration();
        bindSaveSerialization();
    });

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
