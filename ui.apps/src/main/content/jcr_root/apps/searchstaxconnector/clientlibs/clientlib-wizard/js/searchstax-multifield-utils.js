(function (window) {
    "use strict";

    var LOG = "[SearchStax Multifield]";

    function resolveMultifieldElement(el) {
        if (!el) {
            return null;
        }
        if (el.tagName && el.tagName.toLowerCase() === "coral-multifield") {
            return el;
        }
        return el.querySelector("coral-multifield") || el;
    }

    function findMultifield(options) {
        var el = null;
        var multifields;
        var i;

        options = options || {};

        if (options.id) {
            el = document.getElementById(options.id)
                || document.querySelector("[data-granite-id='" + options.id + "']");
            el = resolveMultifieldElement(el);
        }

        if (!el && options.fieldName) {
            multifields = document.querySelectorAll("coral-multifield");
            for (i = 0; i < multifields.length; i++) {
                if (multifields[i].querySelector("coral-pathfield[name='./" + options.fieldName + "']")
                        || multifields[i].querySelector("[name='./" + options.fieldName + "']")) {
                    return multifields[i];
                }
            }
        }

        if (!el && options.index !== undefined) {
            multifields = document.querySelectorAll("coral-multifield");
            el = multifields[options.index] || null;
        }

        return el;
    }

    function findNamedField(scope, name) {
        var root = scope || document;
        var selectors = [
            "coral-pathfield[name='./" + name + "']",
            "coral-pathfield[name='" + name + "']",
            "coral-numberinput[name='./" + name + "']",
            "coral-checkbox[name='./" + name + "']",
            "input[name='./" + name + "']",
            "[name='./" + name + "']"
        ];
        var i;

        for (i = 0; i < selectors.length; i++) {
            var candidate = root.querySelector(selectors[i]);
            if (candidate) {
                return candidate;
            }
        }

        return null;
    }

    function dispatchChange(el) {
        if (!el) {
            return;
        }
        el.dispatchEvent(new Event("change", { bubbles: true }));
        el.dispatchEvent(new Event("input", { bubbles: true }));
    }

    function setFieldValue(container, name, value) {
        var field = findNamedField(container, name);
        var normalized = value === undefined || value === null ? "" : String(value);

        if (!field) {
            return false;
        }

        Coral.commons.ready(field, function (el) {
            var currentValue = el.value !== undefined ? el.value : "";
            var inner = el.querySelector(
                "input[is='coral-textfield'], input[type='text'], input:not([type='hidden']), textarea");
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

            dispatchChange(el);
        });

        return true;
    }

    function setCheckboxValue(container, name, checked) {
        var field = findNamedField(container, name);
        if (!field) {
            return false;
        }

        Coral.commons.ready(field, function (el) {
            var normalizedChecked = Boolean(checked);
            if (el.checked !== undefined && el.checked === normalizedChecked) {
                return;
            }

            if (el.checked !== undefined) {
                el.checked = normalizedChecked;
            }
            dispatchChange(el);
        });

        return true;
    }

    function clearMultifield(multifield, done) {
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

    function populatePathMultifield(multifield, fieldName, values, done) {
        if (!multifield || !values || !values.length) {
            if (done) {
                done(false);
            }
            return;
        }

        clearMultifield(multifield, function () {
            var index = 0;

            function addNext() {
                if (index >= values.length) {
                    if (done) {
                        done(true);
                    }
                    return;
                }

                var value = values[index];
                var itemIndex = index;
                index += 1;

                multifield.items.add();

                setTimeout(function () {
                    var items = multifield.items.getAll();
                    var item = items[itemIndex] || items[items.length - 1];
                    if (!item) {
                        console.warn(LOG, "Multifield item not ready for", fieldName, "at index", itemIndex);
                        setTimeout(addNext, 150);
                        return;
                    }

                    setFieldValue(item, fieldName, value);
                    setTimeout(addNext, 150);
                }, 250);
            }

            addNext();
        });
    }

    function populateIncludePaths(multifield, includePaths, done) {
        if (!multifield || !includePaths || !includePaths.length) {
            if (done) {
                done(false);
            }
            return;
        }

        clearMultifield(multifield, function () {
            var index = 0;

            function addNext() {
                if (index >= includePaths.length) {
                    if (done) {
                        done(true);
                    }
                    return;
                }

                var includePath = includePaths[index];
                var itemIndex = index;
                index += 1;

                multifield.items.add();

                setTimeout(function () {
                    var items = multifield.items.getAll();
                    var item = items[itemIndex] || items[items.length - 1];
                    if (!item) {
                        setTimeout(addNext, 150);
                        return;
                    }

                    setFieldValue(item, "path", includePath.path);
                    setCheckboxValue(item, "includeChildPath", includePath.includeChildPath);
                    setTimeout(addNext, 150);
                }, 250);
            }

            addNext();
        });
    }

    window.SearchStaxMultifieldUtil = {
        LOG: LOG,
        findMultifield: findMultifield,
        findNamedField: findNamedField,
        setFieldValue: setFieldValue,
        setCheckboxValue: setCheckboxValue,
        clearMultifield: clearMultifield,
        populatePathMultifield: populatePathMultifield,
        populateIncludePaths: populateIncludePaths
    };
})(window);

(function (window, document) {
    "use strict";

    function isServletRequest(url, path) {
        if (!url || !path) {
            return false;
        }
        var normalizedUrl = url.split("?")[0].split("#")[0];
        return normalizedUrl === path || normalizedUrl.endsWith(path);
    }

    function findFormField(name) {
        return document.querySelector("[name='" + name + "']");
    }

    function clearMultifieldForConfig(multifield, done) {
        if (window.SearchStaxMultifieldUtil) {
            window.SearchStaxMultifieldUtil.clearMultifield(multifield, done);
        } else if (done) {
            done();
        }
    }

    function setEnabledSelect(item, enabled) {
        var checkbox = item && item.querySelector("coral-checkbox[name*='enabled'], input[name*='enabled']");
        if (!checkbox) {
            return;
        }
        Coral.commons.ready(checkbox, function (el) {
            if (el.checked !== undefined) {
                el.checked = Boolean(enabled);
            }
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
                if (!value || value === "custom" || value === currentValue) {
                    item.disabled = false;
                } else {
                    item.disabled = used[value] === true;
                }
            }
        });
    }

    function applySecretFieldStates(data, secretFields) {
        if (!data || !secretFields) {
            return;
        }
        secretFields.forEach(function (fieldName) {
            var field = findFormField(fieldName);
            if (!field) {
                return;
            }
            Coral.commons.ready(field, function (el) {
                el.value = "";
                if (data[fieldName + "Configured"]) {
                    el.setAttribute("placeholder", "Value saved — leave blank to keep");
                } else {
                    el.removeAttribute("placeholder");
                }
            });
        });
    }

    function triggerFieldValidation(el, isValid) {
        if (!el) {
            return;
        }
        var $ = window.Granite && window.Granite.$ ? window.Granite.$ : window.jQuery;
        if (!$) {
            return;
        }
        var validationApi = $(el).adaptTo("foundation-validation");
        if (!validationApi) {
            return;
        }
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

    function redirectAfterSave() {
        var href = "/aem/start.html";
        if (document.referrer && document.referrer.indexOf(window.location.host) !== -1) {
            href = document.referrer;
        }

        setTimeout(function () {
            window.location.href = href;
        }, 1500);
    }

    function attachSaveHandlers(savePath, successMessage, validateFn) {
        if (!validateFn) {
            return;
        }
        var form = document.querySelector("form[action*='" + savePath + "']");
        if (!form || form.dataset.searchstaxSaveBound === "true") {
            return;
        }
        form.dataset.searchstaxSaveBound = "true";
        form.addEventListener("submit", function (event) {
            event.preventDefault();
            event.stopImmediatePropagation();

            if (validateFn()) {
                var $ = window.Granite && window.Granite.$ ? window.Granite.$ : window.jQuery;
                if (!$) {
                    return;
                }
                var $form = $(form);
                var url = $form.attr("action");
                var data = $form.serialize();

                $.post(url, data)
                    .done(function () {
                        $(window).adaptTo("foundation-ui")
                            .notify("Success", successMessage || "Configuration saved successfully.", "success");
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

    window.SearchStaxConfigUtil = {
        attachSaveHandlers: attachSaveHandlers,
        applySecretFieldStates: applySecretFieldStates,
        clearMultifield: clearMultifieldForConfig,
        findNamedField: findFormField,
        isServletRequest: isServletRequest,
        setEnabledSelect: setEnabledSelect,
        setSelectOptionsDisabled: setSelectOptionsDisabled,
        showDuplicateWarning: function (message) {
            window.alert(message);
        },
        triggerFieldValidation: triggerFieldValidation
    };
})(window, document);
