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
            if (el.value !== undefined) {
                el.value = normalized;
            }

            var inner = el.querySelector(
                "input[is='coral-textfield'], input[type='text'], input:not([type='hidden']), textarea");
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
            if (el.checked !== undefined) {
                el.checked = Boolean(checked);
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
