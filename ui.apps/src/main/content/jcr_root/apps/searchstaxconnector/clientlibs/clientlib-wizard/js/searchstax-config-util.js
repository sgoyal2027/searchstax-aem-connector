(function (window, document) {
    "use strict";

    var LOG = "[SearchStax Config]";

    function getJQuery() {
        return window.Granite && window.Granite.$ ? window.Granite.$ : window.jQuery;
    }

    function getFoundationUi() {
        try {
            return getJQuery()(window).adaptTo("foundation-ui");
        } catch (e) {
            return null;
        }
    }

    function isServletRequest(url, path) {
        if (!url || !path) {
            return false;
        }
        var normalizedUrl = url.split("?")[0].split("#")[0];
        return normalizedUrl === path || normalizedUrl.endsWith(path);
    }

    function findNamedField(name) {
        var selectors = [
            "coral-textfield[name='" + name + "']",
            "coral-password[name='" + name + "']",
            "coral-numberinput[name='" + name + "']",
            "coral-checkbox[name='" + name + "']",
            "coral-select[name='" + name + "']",
            "input[name='" + name + "']",
            "textarea[name='" + name + "']",
            "[name='" + name + "']"
        ];
        var i;

        for (i = 0; i < selectors.length; i++) {
            var el = document.querySelector(selectors[i]);
            if (el) {
                return el;
            }
        }

        return null;
    }

    function clearMultifield(multifield, done) {
        if (window.SearchStaxMultifieldUtil && window.SearchStaxMultifieldUtil.clearMultifield) {
            window.SearchStaxMultifieldUtil.clearMultifield(multifield, done);
            return;
        }

        if (!multifield) {
            if (done) {
                done();
            }
            return;
        }

        Coral.commons.ready(multifield, function (mf) {
            var items = mf.items.getAll().slice();
            items.forEach(function (item) {
                mf.items.remove(item);
            });
            if (done) {
                done();
            }
        });
    }

    function setEnabledSelect(item, enabled) {
        if (!item) {
            return;
        }

        var checkbox = item.querySelector("coral-checkbox[name*='enabled']")
            || item.querySelector("input[name*='enabled']");

        if (!checkbox) {
            return;
        }

        Coral.commons.ready(checkbox, function (el) {
            if (el.checked !== undefined) {
                el.checked = Boolean(enabled);
            }
            el.dispatchEvent(new Event("change", { bubbles: true }));
        });
    }

    function setSelectOptionsDisabled(select, used, currentValue) {
        if (!select) {
            return;
        }

        Coral.commons.ready(select, function (el) {
            var items = el.items ? el.items.getAll() : el.querySelectorAll("coral-select-item");
            var i;

            for (i = 0; i < items.length; i++) {
                var item = items[i];
                var value = item.value;

                if (!value || value === "custom") {
                    item.disabled = false;
                    continue;
                }

                if (value === currentValue) {
                    item.disabled = false;
                    continue;
                }

                item.disabled = used[value] === true;
            }
        });
    }

    function showDuplicateWarning(message) {
        var ui = getFoundationUi();
        if (ui && ui.alert) {
            ui.alert("Duplicate Mapping", message, "warning");
            return;
        }

        window.alert(message);
    }

    function applySecretFieldStates(data, secretFields, attempt) {
        if (!data || !secretFields || !secretFields.length) {
            return;
        }

        attempt = attempt || 0;

        secretFields.forEach(function (fieldName) {
            var field = findNamedField(fieldName);
            if (!field) {
                return;
            }

            var configuredKey = fieldName + "Configured";
            var isConfigured = Boolean(data[configuredKey]);

            Coral.commons.ready(field, function (el) {
                el.value = "";

                if (isConfigured) {
                    el.setAttribute("placeholder", "Value saved — leave blank to keep");
                } else {
                    el.removeAttribute("placeholder");
                }
            });
        });
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

    function bindValidation(savePath, validateFn) {
        if (!validateFn) {
            return;
        }

        var form = document.querySelector("form[action*='" + savePath + "']");
        if (!form || form.dataset.searchstaxValidationBound === "true") {
            return;
        }

        form.dataset.searchstaxValidationBound = "true";
        form.addEventListener("submit", function (event) {
            var message = validateFn();
            if (message) {
                event.preventDefault();
                event.stopImmediatePropagation();
                var ui = getFoundationUi();
                if (ui && ui.alert) {
                    ui.alert("Validation Error", message, "error");
                }
                return false;
            }
        }, true);
    }

    function attachSaveHandlers(savePath, successMessage, validateFn) {
        var $ = getJQuery();
        if (!$) {
            console.warn(LOG, "jQuery is not available; save handlers not attached for", savePath);
            return;
        }

        bindValidation(savePath, validateFn);

        $(document).ajaxSuccess(function (event, xhr, settings) {
            if (!isServletRequest(settings.url, savePath)) {
                return;
            }

            if (settings.type && settings.type.toUpperCase() !== "POST") {
                return;
            }

            var ui = getFoundationUi();
            if (ui && ui.notify) {
                ui.notify("Success", successMessage, "success");
            }
            redirectAfterSave();
        });

        $(document).ajaxError(function (event, xhr, settings) {
            if (!isServletRequest(settings.url, savePath)) {
                return;
            }

            if (settings.type && settings.type.toUpperCase() !== "POST") {
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

            var ui = getFoundationUi();
            if (ui && ui.alert) {
                ui.alert("Validation Error", message, "error");
            }
        });
    }

    window.SearchStaxConfigUtil = {
        attachSaveHandlers: attachSaveHandlers,
        applySecretFieldStates: applySecretFieldStates,
        clearMultifield: clearMultifield,
        findNamedField: findNamedField,
        isServletRequest: isServletRequest,
        setEnabledSelect: setEnabledSelect,
        setSelectOptionsDisabled: setSelectOptionsDisabled,
        showDuplicateWarning: showDuplicateWarning
    };
})(window, document);
