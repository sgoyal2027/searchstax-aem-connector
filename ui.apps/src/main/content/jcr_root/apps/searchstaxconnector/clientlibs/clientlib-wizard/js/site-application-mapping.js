(function ($, document) {
    "use strict";

    if (!window.location.pathname.includes("siteapplicationmapping")) {
        return;
    }

    $(document).ready(function () {
        Coral.commons.ready(document, function () {
            $.getJSON(
                "/bin/searchstaxconnector/wizard/site-application-mappings-load",
                function (data) {
                    if (data && data.length > 0) {
                        populateSiteMappings(data);
                    }
                }
            );
        });
    });

    function populateSiteMappings(mappings) {
        var multifield = $("coral-multifield")[0];
        if (!multifield) {
            return;
        }

        mappings.forEach(function (mapping, index) {
            multifield.items.add();

            setTimeout(function () {
                var items = multifield.items.getAll();
                var item = items[index];
                if (!item) {
                    return;
                }

                setFieldValue(item, "siteRootPath", mapping.siteRootPath);
                setFieldValue(item, "updateEndpoint", mapping.updateEndpoint);
                setFieldValue(item, "updateToken", mapping.updateToken);
                setFieldValue(item, "searchProfile", mapping.searchProfile);

                var enabled = item.querySelector("input[name*='enabled']");
                if (enabled) {
                    enabled.checked = mapping.enabled !== false;
                }
            }, 50);
        });
    }

    function setFieldValue(item, fieldName, value) {
        var field = item.querySelector("[name*='/" + fieldName + "']");
        if (field && value != null) {
            field.value = value;
        }
    }
})(Granite.$, document);
