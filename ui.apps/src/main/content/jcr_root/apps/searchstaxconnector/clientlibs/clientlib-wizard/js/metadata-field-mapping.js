(function ($, document) {
  if (!window.location.pathname.includes("/metadatafieldmapping")) {
    return;
  }

  ("use strict");

  var __MF_INITIAL_LOAD = true;

  $(document).ready(function () {
    Coral.commons.ready(document, function () {
      $.getJSON(
        "/bin/searchstaxconnector/wizard/metadata-field-mappings-load",
        function (data) {
          if (data && data.length > 0) {
            populateMetadataMappings(data);
          }

          setTimeout(function () {
            __MF_INITIAL_LOAD = false;
            updateMappingTypeOptions();
          }, 500);
        },
      );
    });
  });

  // ======================================================
  // POPULATE MULTIFIELD
  // ======================================================
  function populateMetadataMappings(mappings) {
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

        var mappingType = item.querySelector(
          "coral-select[name*='mappingType']",
        );

        var fieldType = item.querySelector(
          "coral-select[name*='searchStaxFieldType']",
        );

        var customProperty = item.querySelector(
          "input[name*='customProperty']",
        );

        var indexFieldName = item.querySelector(
          "input[name*='indexFieldName']",
        );

        var enabled = item.querySelector("input[name*='enabled']");
        var mandatory = item.querySelector("input[name*='mandatory']");

        if (customProperty) {
          customProperty.value = mapping.customProperty || "";
          customProperty.dispatchEvent(new Event("change", { bubbles: true }));
        }

        if (indexFieldName) {
          indexFieldName.value = mapping.searchStaxField || "";

          indexFieldName.dispatchEvent(new Event("input", { bubbles: true }));
        }

        if (mappingType) {
          Coral.commons.ready(mappingType, function () {
            mappingType.value = mapping.aemField;

            mappingType.__initTriggered = true;

            mappingType.dispatchEvent(new Event("change", { bubbles: true }));
          });
        }
        if (fieldType) {
          Coral.commons.ready(fieldType, function () {

            fieldType.value = mapping.searchStaxFieldType || "";

            setTimeout(function () {
              fieldType.value = mapping.searchStaxFieldType || "";
            }, 100);
          });
        }
        if (enabled) {
          enabled.checked = mapping.enabled;
        }
        if (mandatory) {
          mandatory.checked = mapping.mandatory;
        }
      }, 50);
    });
  }

  // ======================================================
  // CORE LOGIC
  // ======================================================
  function handleMappingTypeChange(item, isInitial) {
    var mappingType = item.querySelector("coral-select");
    var customProperty = item.querySelector("input[name*='customProperty']");
    var indexFieldName = item.querySelector("input[name*='indexFieldName']");

    if (!mappingType) {
      return;
    }

    var value = mappingType.value;
    var container = customProperty ? customProperty.closest("div") : null;

    // CUSTOM CASE
    if (value === "custom") {
      if (container) {
        container.style.display = "block";
      }

      // DO NOT CLEAR EXISTING VALUES
      return;
    }

    if (container) {
      container.style.display = "none";
    }

    if (customProperty) {
      customProperty.value = value;
    }

    // INIT MODE → DO NOTHING
    if (isInitial) {
      return;
    }

    var formattedValue = value;

    if (value && value.indexOf(":") !== -1) {
      formattedValue = value.split(":")[1];
    }

    if (indexFieldName) {
      indexFieldName.value = formattedValue;

      indexFieldName.dispatchEvent(new Event("input", { bubbles: true }));

      indexFieldName.dispatchEvent(new Event("change", { bubbles: true }));
    }
  }
  // ======================================================
  // PREVENT DUPLICATE MAPPING TYPES
  // ======================================================
  function updateMappingTypeOptions() {
    var selectedValues = [];

    $("coral-multifield-item").each(function () {
      var mappingSelect = $(this).find("coral-select[name*='mappingType']")[0];

      if (!mappingSelect) {
        return;
      }

      var value = mappingSelect.value;

      if (value && value !== "custom") {
        selectedValues.push(value);
      }
    });

    $("coral-multifield-item").each(function () {
      var mappingSelect = $(this).find("coral-select[name*='mappingType']")[0];

      if (!mappingSelect) {
        return;
      }

      var currentValue = mappingSelect.value;

      Coral.commons.ready(mappingSelect, function () {
        mappingSelect.items.getAll().forEach(function (item) {
          var optionValue = item.value;

          // Custom is always allowed
          if (optionValue === "custom") {
            item.disabled = false;
            return;
          }

          // Keep current selection enabled
          if (optionValue === currentValue) {
            item.disabled = false;
            return;
          }

          item.disabled = selectedValues.indexOf(optionValue) !== -1;
        });
      });
    });
  }
  // ======================================================
  // USER CHANGE LISTENER
  // ======================================================
  $(document).on("change", "coral-select", function () {
    var item = $(this).closest("coral-multifield-item")[0];

    if (!item) {
      return;
    }

    // Ignore initialization events
    if (this.__initTriggered) {
      this.__initTriggered = false;
      return;
    }

    // Only process mappingType dropdown
    var selectName = this.getAttribute("name") || "";

    if (selectName.indexOf("mappingType") === -1) {
      return;
    }
    handleMappingTypeChange(item, false);
    updateMappingTypeOptions();
  });
  // ======================================================
  // MULTIFIELD ADD
  // ======================================================
  $(document).on("click", "button[coral-multifield-add]", function () {
    setTimeout(function () {
      updateMappingTypeOptions();
    }, 200);
  });
})(Granite.$, document);
