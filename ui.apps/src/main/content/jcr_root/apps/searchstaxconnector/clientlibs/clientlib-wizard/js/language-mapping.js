(function ($, document) {

    if (!window.location.pathname.includes("/languagemapping")) {
        return;
    }

    "use strict";

    var KNOWN_AEM_LANGUAGES = [
        "en", "en_US", "en_GB", "de", "de_DE", "fr", "fr_FR", "es", "es_ES",
        "it", "it_IT", "pt", "pt_BR", "nl", "nl_NL", "ja", "ja_JP", "zh",
        "zh_CN", "zh_TW", "ko", "ko_KR", "ar", "ru"
    ];

    var MAX_LOAD_ATTEMPTS = 20;

    function loadLanguageMappings(attempt) {
        attempt = attempt || 0;

        $.getJSON(
            "/bin/searchstaxconnector/wizard/language-mappings-load",
            function (data) {
                var multifield = document.querySelector("coral-multifield");

                if (!multifield) {
                    if (attempt < MAX_LOAD_ATTEMPTS) {
                        setTimeout(function () {
                            loadLanguageMappings(attempt + 1);
                        }, 200);
                    }
                    return;
                }

                if (data && data.length > 0) {
                    populateLanguageMappings(data);
                } else {
                    initializeAllMultifieldItems();
                }
            }
        ).fail(function () {
            if (attempt < MAX_LOAD_ATTEMPTS) {
                setTimeout(function () {
                    loadLanguageMappings(attempt + 1);
                }, 200);
            }
        });
    }

    $(document).ready(function () {

        Coral.commons.ready(document, function () {
            loadLanguageMappings(0);
        });

        SearchStaxConfigUtil.attachSaveHandlers(
            "/bin/searchstaxconnector/wizard/language-mappings",
            "Language mappings saved successfully.",
            validateLanguageMappingsForm
        );
    });

    document.addEventListener("foundation-contentloaded", function () {
        loadLanguageMappings(0);
    });

    function validateLanguageMappingsForm() {
        var items = document.querySelectorAll("coral-multifield-item");
        var rowNumber;

        for (rowNumber = 0; rowNumber < items.length; rowNumber++) {
            var item = items[rowNumber];
            var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
            var languageValue = aemLanguageType ? (aemLanguageType.value || "").trim() : "";

            if (!languageValue) {
                return "Please select an AEM language for mapping row " + (rowNumber + 1) + ".";
            }

            if (languageValue === "custom" && !readTextFieldValue(item, "customAemLanguage")) {
                return "Please enter a custom AEM language for mapping row " + (rowNumber + 1) + ".";
            }

            if (!readTextFieldValue(item, "searchStaxLanguage")) {
                return "Please enter a SearchStax language for mapping row " + (rowNumber + 1) + ".";
            }
        }

        return null;
    }


    function readTextFieldValue(item, nameFragment) {
        if (!item) {
            return "";
        }

        var coralField = item.querySelector("coral-textfield[name*='" + nameFragment + "']");
        if (coralField && coralField.value) {
            return String(coralField.value).trim();
        }

        var input = item.querySelector("input[name*='" + nameFragment + "']");
        return input ? String(input.value || "").trim() : "";
    }

    function getLanguageMappingKey(item) {
        var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
        if (!aemLanguageType || !aemLanguageType.value) {
            return null;
        }

        if (aemLanguageType.value === "custom") {
            var customLanguage = readTextFieldValue(item, "customAemLanguage");
            return customLanguage ? "custom:" + customLanguage : null;
        }

        return aemLanguageType.value;
    }

    function getUsedLanguageKeys(excludeItem) {
        var used = {};

        $("coral-multifield-item").each(function () {
            if (excludeItem && this === excludeItem) {
                return;
            }
            var key = getLanguageMappingKey(this);
            if (key) {
                used[key] = true;
            }
        });

        return used;
    }

    function isDuplicateLanguageKey(item, candidateKey) {
        if (!candidateKey) {
            return false;
        }
        return getUsedLanguageKeys(item)[candidateKey] === true;
    }

    function refreshLanguageMappingOptions() {
        $("coral-multifield-item").each(function () {
            var item = this;
            var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
            if (!aemLanguageType) {
                return;
            }

            var used = getUsedLanguageKeys(item);
            SearchStaxConfigUtil.setSelectOptionsDisabled(aemLanguageType, used, aemLanguageType.value);
        });
    }

    function resolveAemLanguageType(aemLanguage) {

        if (!aemLanguage) {
            return "en";
        }

        for (var i = 0; i < KNOWN_AEM_LANGUAGES.length; i++) {
            if (KNOWN_AEM_LANGUAGES[i] === aemLanguage) {
                return aemLanguage;
            }
        }

        return "custom";
    }

    function initializeMultifieldItem(item) {

        SearchStaxConfigUtil.setEnabledSelect(item, false);

        var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");

        if (!aemLanguageType) {
            return;
        }

        Coral.commons.ready(aemLanguageType, function () {

            var value = aemLanguageType.value || "";

            item.__lastAemLanguageType = value;
            item.__lastCustomAemLanguage = readTextFieldValue(item, "customAemLanguage");

            toggleCustomLanguageField(item, value);
            refreshLanguageMappingOptions();
        });
    }

    function initializeAllMultifieldItems() {

        $("coral-multifield-item").each(function () {
            initializeMultifieldItem(this);
        });
    }

    function setItemFieldValue(item, nameFragment, value) {
        var field = item.querySelector(
            "coral-textfield[name*='" + nameFragment + "'], input[name*='" + nameFragment + "']"
        );

        if (!field) {
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

            el.dispatchEvent(new Event("input", { bubbles: true }));
            el.dispatchEvent(new Event("change", { bubbles: true }));
        });
    }

    function populateLanguageMappings(mappings) {

        var multifield = $("coral-multifield")[0];

        if (!multifield) {
            return;
        }

        SearchStaxConfigUtil.clearMultifield(multifield, function () {
            var index = 0;

            function addNext() {
                if (index >= mappings.length) {
                    return;
                }

                var mapping = mappings[index];
                var itemIndex = index;
                index += 1;

                multifield.items.add();

                setTimeout(function () {
                    var items = multifield.items.getAll();
                    var item = items[itemIndex];

                    if (!item) {
                        setTimeout(addNext, 150);
                        return;
                    }

                    var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
                    var resolvedType = resolveAemLanguageType(mapping.aemLanguage);

                    setItemFieldValue(item, "searchStaxLanguage", mapping.searchStaxLanguage);

                    if (resolvedType === "custom") {
                        setItemFieldValue(
                            item,
                            "customAemLanguage",
                            mapping.customAemLanguage || mapping.aemLanguage
                        );
                    }

                    if (aemLanguageType) {
                        Coral.commons.ready(aemLanguageType, function () {
                            aemLanguageType.value = resolvedType;
                            aemLanguageType.__initTriggered = true;
                            aemLanguageType.dispatchEvent(new Event("change", { bubbles: true }));
                        });
                    }

                    SearchStaxConfigUtil.setEnabledSelect(item, mapping.enabledLanguageMapping);

                    item.__lastAemLanguageType = resolvedType || "";
                    item.__lastCustomAemLanguage = resolvedType === "custom"
                        ? (mapping.customAemLanguage || mapping.aemLanguage || "")
                        : "";
                    refreshLanguageMappingOptions();

                    setTimeout(addNext, 150);
                }, 250);
            }

            addNext();
        });
    }

    function toggleCustomLanguageField(item, value) {

        var customAemLanguage = item.querySelector("input[name*='customAemLanguage']");
        var container = customAemLanguage ? customAemLanguage.closest("div") : null;

        if (!container) {
            return;
        }

        if (value === "custom") {
            container.style.display = "block";
        } else {
            container.style.display = "none";
        }
    }

    function handleAemLanguageChange(item) {

        var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
        var customAemLanguage = item.querySelector("input[name*='customAemLanguage']");
        var searchStaxLanguage = item.querySelector("input[name*='searchStaxLanguage']");

        if (!aemLanguageType) {
            return;
        }

        var value = aemLanguageType.value;

        if (!value) {
            toggleCustomLanguageField(item, value);
            return;
        }

        toggleCustomLanguageField(item, value);

        if (value === "custom") {
            if (customAemLanguage) {
                customAemLanguage.value = "";
            }
            if (searchStaxLanguage) {
                searchStaxLanguage.value = "";
            }
            return;
        }

        if (searchStaxLanguage) {
            searchStaxLanguage.value = value.split("_")[0];
            searchStaxLanguage.dispatchEvent(new Event("input", { bubbles: true }));
            searchStaxLanguage.dispatchEvent(new Event("change", { bubbles: true }));
        }
    }

    $(document).on("change", "coral-select[name*='aemLanguageType']", function () {

        var item = $(this).closest("coral-multifield-item")[0];

        if (!item) {
            return;
        }

        if (this.__initTriggered) {
            this.__initTriggered = false;
            toggleCustomLanguageField(item, this.value);
            refreshLanguageMappingOptions();
            return;
        }

        var selectedValue = this.value;
        if (selectedValue && selectedValue !== "custom" &&
                isDuplicateLanguageKey(item, selectedValue)) {
            SearchStaxConfigUtil.showDuplicateWarning(
                "This AEM language is already mapped in another row.");
            this.value = item.__lastAemLanguageType || "";
            return;
        }

        item.__lastAemLanguageType = selectedValue || "";
        handleAemLanguageChange(item);
        refreshLanguageMappingOptions();
    });

    $(document).on("change input", "input[name*='customAemLanguage'], coral-textfield[name*='customAemLanguage']", function () {
        var item = $(this).closest("coral-multifield-item")[0];
        if (!item) {
            return;
        }

        var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
        if (!aemLanguageType || aemLanguageType.value !== "custom") {
            return;
        }

        var candidateKey = getLanguageMappingKey(item);
        if (candidateKey && isDuplicateLanguageKey(item, candidateKey)) {
            SearchStaxConfigUtil.showDuplicateWarning(
                "This custom AEM language is already mapped in another row.");
            var input = item.querySelector("input[name*='customAemLanguage'], coral-textfield[name*='customAemLanguage']");
            if (input) {
                input.value = item.__lastCustomAemLanguage || "";
            }
            return;
        }

        item.__lastCustomAemLanguage = readTextFieldValue(item, "customAemLanguage");
        refreshLanguageMappingOptions();
    });

    $(document).on("click", "[coral-multifield-add]", function () {

        setTimeout(function () {

            var items = $("coral-multifield-item");

            if (items.length > 0) {
                initializeMultifieldItem(items[items.length - 1]);
            }

        }, 500);
    });

    $(document).on("click", "[coral-multifield-remove]", function () {
        setTimeout(refreshLanguageMappingOptions, 300);
    });

})(window.Granite && window.Granite.$ ? window.Granite.$ : window.jQuery, document);
