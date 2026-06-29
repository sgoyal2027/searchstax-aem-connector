(function (document) {
    "use strict";

    var LOG = "[SearchStax Wizard]";
    var GENERAL_TEST_BUTTON_ID = "searchstax-general-test-button";

    /* ------------------------------------------------------------------ */
    /* DOM helpers                                                          */
    /* ------------------------------------------------------------------ */

    function resolveGeneralTestButton(target) {
        var el = target;
        while (el && el !== document) {
            if (el.getAttribute) {
                var gid = el.getAttribute("data-granite-id") || el.getAttribute("id");
                if (gid === GENERAL_TEST_BUTTON_ID) {
                    return el;
                }
            }
            el = el.parentNode;
        }
        return null;
    }

    function findField(name) {
        // Try coral wrapper first, then plain input
        var selectors = [
            "coral-textfield[name='" + name + "']",
            "coral-password[name='" + name + "']",
            "coral-numberinput[name='" + name + "']",
            "coral-checkbox[name='" + name + "']",
            "input[name='" + name + "']",
            "textarea[name='" + name + "']",
            "[name='" + name + "']"
        ];
        for (var i = 0; i < selectors.length; i++) {
            var el = document.querySelector(selectors[i]);
            if (el) {
                return el;
            }
        }
        console.warn(LOG, "Field not found for name:", name);
        return null;
    }

    function getFieldValue(el) {
        if (!el) { return ""; }
        // Coral wraps a native input — try inner input first
        var inner = el.querySelector("input:not([type='hidden']), textarea");
        var val = inner ? inner.value : el.value;
        return (val || "").trim();
    }

    function isChecked(el) {
        if (!el) { return false; }
        var inner = el.querySelector("input[type='checkbox']");
        if (inner) { return inner.checked; }
        // coral-checkbox has a .checked property
        if (typeof el.checked === "boolean") { return el.checked; }
        return el.hasAttribute("checked");
    }

    /* ------------------------------------------------------------------ */
    /* UI helpers                                                           */
    /* ------------------------------------------------------------------ */

    function sanitizeIdPart(value) {
        return String(value || "").replace(/[^a-zA-Z0-9\-_]/g, "-");
    }

    function ensureContainer(id, anchor, marginTop) {
        var safeId = sanitizeIdPart(id);
        var el = document.getElementById(safeId);
        if (!el) {
            el = document.createElement("div");
            el.id = safeId;
            el.style.marginTop = marginTop || "12px";
            anchor.parentNode.appendChild(el);
        }
        return el;
    }

    function renderResult(anchor, resultId, isSuccess, message, details) {
        var container = ensureContainer(resultId, anchor, "12px");
        var type = isSuccess ? "success" : "error";
        container.innerHTML = "<coral-alert variant='" + type + "'>"
            + "<coral-alert-content>"
            + "<strong>" + (isSuccess ? "Success" : "Failure") + ":</strong> "
            + message
            + (details ? "<div style='margin-top:6px;color:#666;font-size:12px;'>" + details + "</div>" : "")
            + "</coral-alert-content></coral-alert>";
    }

    function setButtonText(button, text) {
        var label = button.querySelector("coral-button-label");
        if (label) {
            label.textContent = text;
        } else if (button.label && button.label.textContent !== undefined) {
            button.label.textContent = text;
        } else {
            button.textContent = text;
        }
    }

    /* ------------------------------------------------------------------ */
    /* CSRF                                                                 */
    /* ------------------------------------------------------------------ */

    function getCsrfToken() {
        return fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (d) { return (d && d.token) ? d.token : ""; })
            .catch(function () { return ""; });
    }

    /* ------------------------------------------------------------------ */
    /* Core init                                                            */
    /* ------------------------------------------------------------------ */

    var delegatedBound = false;

    function isValidHttpUrl(value) {
        if (!value) {
            return false;
        }
        try {
            var parsed = new URL(value);
            return parsed.protocol === "http:" || parsed.protocol === "https:";
        } catch (e) {
            return false;
        }
    }

    function triggerFieldValidation(el, isValid) {
        if (!el) { return; }
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

   function handleGeneralConfigurationTest(button) {
    var endpointEl = findField("endpointUrl");
    var tokenEl = findField("apiToken");

    var resultId = "searchstax-test-result-general";
    var urlId = "searchstax-test-url-general";
    ensureContainer(resultId, button, "12px");
    var urlLabel = ensureContainer(urlId, button, "8px");

    if (!endpointEl || !tokenEl) {
        renderResult(button, resultId, false, "Connector fields are not available.");
        return;
    }

    var endpointUrl = getFieldValue(endpointEl);
    var token = getFieldValue(tokenEl);

    var hasSavedToken = false;
    if (tokenEl) {
        var innerToken = tokenEl.querySelector("input:not([type='hidden']), textarea");
        var placeholder = tokenEl.getAttribute("placeholder") || (innerToken && innerToken.getAttribute("placeholder"));
        if (placeholder && placeholder.indexOf("Value saved") !== -1) {
            hasSavedToken = true;
        }
    }

    triggerFieldValidation(endpointEl, Boolean(endpointUrl));
    triggerFieldValidation(tokenEl, Boolean(token) || hasSavedToken);

    urlLabel.textContent = endpointUrl ? ("Test URL: " + endpointUrl) : "";

    if (!endpointUrl && (!token && !hasSavedToken)) {
        renderResult(button, resultId, false, "Endpoint and token are required.");
        return;
    } else if (!endpointUrl) {
        renderResult(button, resultId, false, "Endpoint URL is required.");
        return;
    } else if (!token && !hasSavedToken) {
        renderResult(button, resultId, false, "API Token is required.");
        return;
    }

    if (!isValidHttpUrl(endpointUrl)) {
        triggerFieldValidation(endpointEl, false);
        renderResult(button, resultId, false, "Endpoint must be a valid http(s) URL.");
        return;
    }

    button.disabled = true;
    setButtonText(button, "Testing...");

    // Prepare payload to send to the OSGi Servlet
    var payload = new URLSearchParams();
    payload.append("endpointUrl", endpointUrl);
    payload.append("endpointType", "general");
    payload.append("apiToken", token); // Backend will intercept and encrypt this

    getCsrfToken()
        .then(function (csrfToken) {
            var headers = { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" };
            if (csrfToken) { headers["CSRF-Token"] = csrfToken; }
            return fetch("/bin/staxsync/searchstax/test-connection", {
                method: "POST",
                body: payload.toString(),
                headers: headers,
                credentials: "same-origin"
            });
        })
        .then(function (response) {
            return response.text().then(function (text) {
                try { return JSON.parse(text); } catch (e) {
                    return { success: false, message: "Unexpected response from server." };
                }
            });
        })
        .then(function (data) {
            var status = data.targetStatus !== undefined ? data.targetStatus : "N/A";
            var details = "HTTP status: " + status;
            renderResult(button, resultId, Boolean(data.success), data.message || "Test completed.", details);
        })
        .catch(function (err) {
            renderResult(button, resultId, false, "Unable to reach test endpoint: " + err.message);
        })
        .finally(function () {
            button.disabled = false;
            setButtonText(button, "Test Configuration");
        });
}

    function attachDelegatedClickOnce() {
        if (delegatedBound) {
            return;
        }
        delegatedBound = true;
        console.log(LOG, "Attached delegated click handler (wizard-safe)");

        document.addEventListener("click", function (e) {
            var button = resolveGeneralTestButton(e.target);
            if (!button) {
                return;
            }

            e.preventDefault();
            e.stopImmediatePropagation();

            console.log(LOG, "General test button clicked");
            handleGeneralConfigurationTest(button);
        }, true);
    }

    /* ------------------------------------------------------------------ */
    /* Bootstrap — wizard panels can appear after first content load       */
    /* ------------------------------------------------------------------ */

    document.addEventListener("foundation-contentloaded", function () {
        console.log(LOG, "foundation-contentloaded fired");
        attachDelegatedClickOnce();
    });

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            console.log(LOG, "DOMContentLoaded fired");
            attachDelegatedClickOnce();
        });
    } else {
        console.log(LOG, "Document already ready — attach handler");
        attachDelegatedClickOnce();
    }

})(document);

(function(document, $) {
   console.log("[SearchStax Wizard] Initializing show/hide logic for custom field");
   "use strict";

   function toggleFields(multifieldItem) {

        var select = multifieldItem.find("coral-select");

        var wrapper = multifieldItem.find(".custom-field-showhide-target, .custom-language-showhide-target");

        if (!select.length || !wrapper.length) {
            return;
        }

       var value = select.val();

       if (value === "custom") {
           wrapper.show();
       } else {
           wrapper.hide();
       }
   }

   function initializeAll() {

       $("coral-multifield-item").each(function () {
           toggleFields($(this));
       });

   }

   // Dropdown change
   $(document).on("change", "coral-select", function () {

       var multifieldItem = $(this).closest("coral-multifield-item");

       toggleFields(multifieldItem);

   });

   // Multifield add
   $(document).on("click", "[coral-multifield-add]", function () {

       setTimeout(function () {
           initializeAll();
       }, 500);

   });

   // Initial page load
   $(window).on("load", function () {

       setTimeout(function () {
           initializeAll();
       }, 500);

   });

})(document, Granite.$);