(function ($, document) {
  if (!window.location.pathname.includes("/metadatafieldmapping")) {
    return;
  }

  ("use strict");

  var __MF_INITIAL_LOAD = true;
  var SAVE_PATH = "/bin/searchstaxconnector/wizard/metadata-field-mappings";

  $(document).ready(function () {
    Coral.commons.ready(document, function () {
      SearchStaxConfigUtil.attachSaveHandlers(
        SAVE_PATH,
        "Metadata field mapping configuration saved successfully.",
        validateMetadataMappingsForm
      );
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

  document.addEventListener("foundation-contentloaded", function () {
    SearchStaxConfigUtil.attachSaveHandlers(
      SAVE_PATH,
      "Metadata field mapping configuration saved successfully.",
      validateMetadataMappingsForm
    );
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
          "coral-textfield[name*='customProperty']",
        ) || item.querySelector("[name*='customProperty']");

        var indexFieldName = item.querySelector(
          "coral-textfield[name*='indexFieldName']",
        ) || item.querySelector("[name*='indexFieldName']");

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
    var customProperty = item.querySelector("coral-textfield[name*='customProperty']") || item.querySelector("[name*='customProperty']");
    var indexFieldName = item.querySelector("coral-textfield[name*='indexFieldName']") || item.querySelector("[name*='indexFieldName']");

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

      if (customProperty) {
        customProperty.value = "";
        triggerFieldValidation(customProperty, true);
      }

      if (indexFieldName) {
        indexFieldName.value = "";
        triggerFieldValidation(indexFieldName, true);
      }

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

  // ======================================================
  // VALIDATION & SAVE HANDLER
  // ======================================================
  $(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
    selector: "coral-select[name*='mappingType']",
    validate: function (el) {
      var val = (el.value || "").trim();
      if (!val) {
        return "Error: AEM metadata field is required";
      }
    }
  });

  $(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
    selector: "coral-textfield[name*='customProperty']",
    validate: function (el) {
      var item = el.closest("coral-multifield-item");
      if (item) {
        var mappingTypeSelect = item.querySelector("coral-select[name*='mappingType']");
        var mappingTypeValue = mappingTypeSelect ? (mappingTypeSelect.value || "").trim() : "";
        if (mappingTypeValue === "custom") {
          var val = (el.value || "").trim();
          if (!val) {
            return "Error: AEM metadata field is required";
          }
        }
      }
    }
  });

  function triggerFieldValidation(el, isValid) {
    if (!el) {
      return;
    }
    var validationApi = $(el).adaptTo("foundation-validation");
    if (validationApi) {
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
  }

  function validateMetadataMappingsForm() {
    var items = document.querySelectorAll("coral-multifield-item");
    var isValid = true;
    var firstInvalidField = null;

    var seenCustomProperties = {};
    var seenIndexFieldNames = {};

    for (var i = 0; i < items.length; i++) {
      var item = items[i];

      var mappingTypeSelect = item.querySelector("coral-select[name*='mappingType']");
      var customPropertyInput = item.querySelector("coral-textfield[name*='customProperty']") || item.querySelector("[name*='customProperty']");
      var indexFieldNameInput = item.querySelector("coral-textfield[name*='indexFieldName']") || item.querySelector("[name*='indexFieldName']");

      var mappingTypeValue = mappingTypeSelect ? (mappingTypeSelect.value || "").trim() : "";
      var mappingTypeValid = Boolean(mappingTypeValue);
      triggerFieldValidation(mappingTypeSelect, mappingTypeValid);
      if (!mappingTypeValid) {
        isValid = false;
        if (!firstInvalidField) {
          firstInvalidField = mappingTypeSelect;
        }
      }

      if (mappingTypeValue === "custom") {
        var customPropValue = customPropertyInput ? (customPropertyInput.value || "").trim() : "";
        var customPropValid = Boolean(customPropValue);

        if (customPropValid) {
          var lowerCustomProp = customPropValue.toLowerCase();
          if (seenCustomProperties[lowerCustomProp]) {
            customPropValid = false;
            $(window).adaptTo("foundation-ui").alert(
              "Validation Error",
              "AEM Metadata Field '" + customPropValue + "' is mapped more than once.",
              "error"
            );
          } else {
            seenCustomProperties[lowerCustomProp] = true;
          }
        }

        triggerFieldValidation(customPropertyInput, customPropValid);
        if (!customPropValid) {
          isValid = false;
          if (!firstInvalidField) {
            firstInvalidField = customPropertyInput;
          }
        }
      } else {
        triggerFieldValidation(customPropertyInput, true);
      }

      var indexFieldNameValue = indexFieldNameInput ? (indexFieldNameInput.value || "").trim() : "";
      var indexFieldNameValid = Boolean(indexFieldNameValue);

      if (indexFieldNameValid) {
        var formatRegex = /^[a-zA-Z_][a-zA-Z0-9_]*$/;
        if (!formatRegex.test(indexFieldNameValue)) {
          indexFieldNameValid = false;
          $(window).adaptTo("foundation-ui").alert(
            "Validation Error",
            "SearchStax Index Field Name '" + indexFieldNameValue + "' is invalid. It must start with a letter or underscore, and contain only letters, numbers, and underscores.",
            "error"
          );
        } else {
          var lowerIndexFieldName = indexFieldNameValue.toLowerCase();
          if (seenIndexFieldNames[lowerIndexFieldName]) {
            indexFieldNameValid = false;
            $(window).adaptTo("foundation-ui").alert(
              "Validation Error",
              "SearchStax Index Field Name '" + indexFieldNameValue + "' is mapped more than once.",
              "error"
            );
          } else {
            seenIndexFieldNames[lowerIndexFieldName] = true;
          }
        }
      }

      triggerFieldValidation(indexFieldNameInput, indexFieldNameValid);
      if (!indexFieldNameValid) {
        isValid = false;
        if (!firstInvalidField) {
          firstInvalidField = indexFieldNameInput;
        }
      }
    }

    if (firstInvalidField && typeof firstInvalidField.scrollIntoView === "function") {
      firstInvalidField.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    return isValid;
  }

})(Granite.$, document);
