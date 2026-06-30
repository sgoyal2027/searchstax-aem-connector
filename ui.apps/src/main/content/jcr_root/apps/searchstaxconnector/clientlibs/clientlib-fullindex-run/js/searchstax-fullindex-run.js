(function (document) {
    "use strict";

    var LOG = "[SearchStax Full Index Run]";
    var BUTTON_ID = "searchstax-run-full-index-button";
    var RESULT_ID = "searchstax-full-index-run-result";
    var PROGRESS_ID = "searchstax-full-index-progress";
    var RUN_ENDPOINT = "/bin/searchstaxconnector/wizard/fullindex-run";
    var STATUS_ENDPOINT = "/bin/searchstaxconnector/wizard/fullindex-status";
    var POLL_INTERVAL_MS = 2000;

    function resolveRunButton(target) {
        if (!target || typeof target.closest !== "function") {
            return null;
        }
        return target.closest("#" + BUTTON_ID)
            || target.closest("[data-granite-id='" + BUTTON_ID + "']")
            || target.closest("[id='" + BUTTON_ID + "']");
    }

    function ensureContainer(id, anchor) {
        var el = document.getElementById(id);
        if (!el) {
            el = document.createElement("div");
            el.id = id;
            el.style.marginTop = "12px";
            anchor.parentNode.appendChild(el);
        }
        return el;
    }

    function findRunButton() {
        return document.getElementById(BUTTON_ID)
            || document.querySelector("[data-granite-id='" + BUTTON_ID + "']")
            || document.querySelector("[id='" + BUTTON_ID + "']");
    }

    function renderResult(anchor, variant, message, details, title) {
        var container = ensureContainer(RESULT_ID, anchor);
        var type = variant || "success";
        var heading = title || (type === "error" ? "Failure" : "Success");
        container.innerHTML = "<div class='searchstax-fullindex-result searchstax-fullindex-result--" + type + "'>"
            + "<coral-alert variant='" + type + "' class='searchstax-fullindex-result-alert'>"
            + "<coral-alert-content>"
            + "<div class='searchstax-fullindex-result-title'><strong>" + heading + ":</strong> " + message + "</div>"
            + (details ? "<div class='searchstax-fullindex-result-details'>" + details + "</div>" : "")
            + "</coral-alert-content></coral-alert></div>";
    }

    function clearProgress(anchor) {
        var container = ensureContainer(PROGRESS_ID, anchor);
        if (container) {
            container.innerHTML = "";
        }
    }

    function clearResult(anchor) {
        var container = ensureContainer(RESULT_ID, anchor);
        if (container) {
            container.innerHTML = "";
        }
    }

    function truncateText(value, maxLength) {
        var text = value || "";
        if (text.length <= maxLength) {
            return text;
        }
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        var marker = "...";
        var keep = maxLength - marker.length;
        var front = Math.floor(keep / 2);
        var back = keep - front;
        return text.substring(0, front) + marker + text.substring(text.length - back);
    }

    function formatElapsed(ms) {
        var totalSeconds = Math.floor(Math.max(0, Number(ms) || 0) / 1000);
        var mins = Math.floor(totalSeconds / 60);
        var secs = totalSeconds % 60;
        return mins + "m " + secs + "s";
    }

    function resolveElapsedMs(status) {
        var poller = getGlobalPoller();
        if (poller && poller.startedAtMs) {
            return Math.max(0, Date.now() - poller.startedAtMs);
        }
        return status.elapsedMs;
    }

    function formatTimestamp(ms) {
        if (!ms) {
            return "";
        }
        try {
            return new Date(ms).toLocaleString();
        } catch (e) {
            return "";
        }
    }

    function renderProgress(anchor, status) {
        var container = ensureContainer(PROGRESS_ID, anchor);
        if (!container) {
            return;
        }
        var lastPath = truncateText(status.lastIndexedPath || "", 120);
        var completedAt = formatTimestamp(status.completedAt);
        var details = "Indexed: " + (status.totalProcessed || 0)
            + " | Attempted: " + (status.totalAttempted != null ? status.totalAttempted : (status.totalProcessed || 0) + (status.failureCount || 0))
            + " | Pages: " + (status.pagesIndexed || 0)
            + " | Assets: " + (status.assetsIndexed || 0)
            + " | Batches: " + (status.currentBatchNumber || 0)
            + " | Elapsed: " + formatElapsed(resolveElapsedMs(status));
        var completedText = completedAt ? (" | Completed: " + completedAt) : "";

        container.innerHTML = "<div class='searchstax-fullindex-progress-panel searchstax-fullindex-progress-panel--running'>"
            + "<div class='searchstax-fullindex-progress-bar-wrap'>"
            + "<coral-progress class='searchstax-fullindex-progress-bar' indeterminate size='L'></coral-progress>"
            + "</div>"
            + "<div class='searchstax-fullindex-progress-title'>"
            + (status.message || "Full index running...")
            + "</div>"
            + "<div class='searchstax-fullindex-progress-stats'>" + details + completedText + "</div>"
            + (lastPath
                ? ("<div class='searchstax-fullindex-progress-path'>"
                    + "<span class='searchstax-fullindex-progress-path-label'>Last path:</span> "
                    + lastPath
                    + "</div>")
                : "")
            + "</div>";
    }

    function getGlobalPoller() {
        return window.searchStaxFullIndexPoller || null;
    }

    function stopPolling() {
        var poller = getGlobalPoller();
        if (poller && poller.intervalId) {
            clearInterval(poller.intervalId);
        }
        window.searchStaxFullIndexPoller = null;
    }

    function setPoller(intervalId, activeJobId, startedAtMs) {
        window.searchStaxFullIndexPoller = {
            intervalId: intervalId,
            activeJobId: activeJobId || "",
            startedAtMs: startedAtMs || Date.now()
        };
    }

    function fetchStatus() {
        return fetch(STATUS_ENDPOINT, {
            method: "GET",
            credentials: "same-origin"
        }).then(function (response) {
            return response.text().then(function (text) {
                var data;
                try {
                    data = JSON.parse(text);
                } catch (e) {
                    data = {};
                }
                if (!response.ok) {
                    throw new Error(data.message || ("Status request failed: HTTP " + response.status));
                }
                return data;
            });
        });
    }

    function renderTerminalResult(anchor, status) {
        var state = status.state || "";
        var variant = "success";
        var title = "Success";
        if (state === "PARTIAL_FAILURE") {
            variant = "warning";
            title = "Warning";
        } else if (state === "FAILED") {
            variant = "error";
            title = "Failure";
        }
        var details = "State: " + (state || "N/A")
            + " | Attempted: " + (status.totalAttempted != null ? status.totalAttempted : (status.totalProcessed || 0) + (status.failureCount || 0))
            + " | Indexed: " + (status.totalProcessed || 0)
            + " | Failures: " + (status.failureCount || 0)
            + " | Pages: " + (status.pagesIndexed || 0)
            + " | Assets: " + (status.assetsIndexed || 0)
            + (status.completedAt ? (" | Completed At: " + formatTimestamp(status.completedAt)) : "")
            + " | Duration: " + formatElapsed(resolveElapsedMs(status));
        renderResult(anchor, variant, status.message || "Full index finished.", details, title);
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

    function getCsrfToken() {
        return fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (d) { return (d && d.token) ? d.token : ""; })
            .catch(function () { return ""; });
    }

    function getWizardForm() {
        return document.getElementById("edit-configuration-properties-form")
            || document.querySelector("form[action*='fullindex-save']");
    }

    function findFieldInForm(form, name) {
        if (!form) {
            return null;
        }
        var selectors = [
            "coral-pathfield[name='" + name + "']",
            "coral-checkbox[name='" + name + "']",
            "input[name='" + name + "']",
            "[name='" + name + "']"
        ];
        for (var i = 0; i < selectors.length; i++) {
            var el = form.querySelector(selectors[i]);
            if (el) {
                return el;
            }
        }
        return null;
    }

    function getFieldValue(el) {
        if (!el) {
            return "";
        }
        var inner = el.querySelector("input:not([type='hidden']), textarea");
        var val = inner ? inner.value : el.value;
        return (val || "").trim();
    }

    function isChecked(el) {
        if (!el) {
            return false;
        }
        var inner = el.querySelector("input[type='checkbox']");
        if (inner) {
            return inner.checked;
        }
        if (typeof el.checked === "boolean") {
            return el.checked;
        }
        return el.hasAttribute("checked");
    }

    function collectMultifieldPaths(form, nameFragment, paramName, params) {
        if (!form) {
            return;
        }
        var nodes = form.querySelectorAll(
            "input[name*='" + nameFragment + "'], coral-pathfield[name*='" + nameFragment + "']");
        var seen = {};
        for (var i = 0; i < nodes.length; i++) {
            var val = getFieldValue(nodes[i]);
            if (val && !seen[val]) {
                seen[val] = true;
                params.append(paramName, val);
            }
        }
    }

    function findIncludePathsMultifield(form) {
        // Strategy 1: granite:id rendered as HTML id
        var el = document.getElementById("searchstax-include-paths-multifield");
        if (el) {
            return el;
        }
        // Strategy 2: granite:id rendered as data-granite-id attribute
        el = form.querySelector("[data-granite-id='searchstax-include-paths-multifield']");
        if (el) {
            return el;
        }
        // Strategy 3: Granite UI renders the multifield JCR node name as the HTML name attribute
        el = form.querySelector("coral-multifield[name='./includePaths']")
            || form.querySelector("coral-multifield[name='includePaths']");
        if (el) {
            return el;
        }
        // Strategy 4: structural — find coral-multifield whose items contain an includeChildPath checkbox
        var candidates = form.querySelectorAll("coral-multifield");
        for (var m = 0; m < candidates.length; m++) {
            if (candidates[m].querySelector("[name*='includeChildPath']")) {
                return candidates[m];
            }
        }
        return null;
    }

    function getPathfieldValue(el) {
        if (!el) {
            return "";
        }
        // Coral custom-element property (most reliable in Coral 3)
        if (typeof el.value === "string") {
            var v = el.value.trim();
            if (v) {
                return v;
            }
        }
        // Inner visible input fallback
        return getFieldValue(el);
    }

    function collectIncludePathsWithChildFlags(form, params) {
    if (!form) return;

    var multifield = document.getElementById("searchstax-include-paths-multifield");
    if (!multifield) return;

    var items = multifield.querySelectorAll("coral-multifield-item");

    items.forEach(function (item) {
        var pathField =
            item.querySelector("coral-pathfield input") ||
            item.querySelector("coral-pathfield") ||
            item.querySelector("input[name='./path']");

        var checkbox =
            item.querySelector("coral-checkbox") ||
            item.querySelector("input[type='checkbox'][name='./includeChildPath']");

        var pathVal = "";

        if (pathField) {
            pathVal = (pathField.value || pathField.getAttribute("value") || "").trim();
        }

        if (!pathVal && pathField && pathField.value !== undefined) {
            pathVal = (pathField.value || "").trim();
        }

        if (pathVal) {
            params.append("includePaths", pathVal);

            var checked = false;
            if (checkbox) {
                checked = checkbox.checked === true;
            }

            params.append("includeChildPaths", checked ? "true" : "false");
        }
    });
}

    function buildRunRequestBody(form) {
        var params = new URLSearchParams();
        var rootPath = getFieldValue(findFieldInForm(form, "./rootPath"));
        if (rootPath) {
            params.append("rootPath", rootPath);
        }
        collectIncludePathsWithChildFlags(form, params);
        collectMultifieldPaths(form, "excludePaths", "excludePaths", params);
        return params.toString();
    }

    function handleRunFullIndex(button) {
        stopPolling();
        clearResult(button);
        clearProgress(button);

        button.disabled = true;
        var originalLabel = "Run full indexing";
        setButtonText(button, "Running...");

        var form = getWizardForm();
        var body = buildRunRequestBody(form);

        getCsrfToken()
            .then(function (csrfToken) {
                var headers = { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" };
                if (csrfToken) {
                    headers["CSRF-Token"] = csrfToken;
                }
                return fetch(RUN_ENDPOINT, {
                    method: "POST",
                    body: body,
                    headers: headers,
                    credentials: "same-origin"
                });
            })
            .then(function (response) {
                return response.text().then(function (text) {
                    var data;
                    try {
                        data = JSON.parse(text);
                    } catch (e) {
                        data = {
                            success: false,
                            message: "Unexpected response from server.",
                            solrStatus: response.status
                        };
                    }
                    return { response: response, data: data };
                });
            })
            .then(function (ctx) {
                var data = ctx.data;
                var httpStatus = ctx.response ? ctx.response.status : 0;
                var accepted = Boolean(data.accepted) || httpStatus === 202;
                var details = "HTTP " + (ctx.response ? ctx.response.status : "N/A")
                    + (data.jobId ? (" | jobId: " + data.jobId) : "");
                console.log(LOG, "Full index run result:", data);
                if (accepted || Boolean(data.success)) {
                    renderResult(
                        button,
                        "success",
                        data.message || "Full index started in background.",
                        details,
                        "Success"
                    );
                    startPolling(button, originalLabel, data.jobId || "", Date.now());
                    return;
                }
                renderResult(button, "error", data.message || "Request completed.", details, "Failure");
                button.disabled = false;
                setButtonText(button, originalLabel);
            })
            .catch(function (err) {
                console.error(LOG, "Full index run fetch error:", err);
                renderResult(button, "error", "Unable to reach server: " + err.message, "", "Failure");
                button.disabled = false;
                setButtonText(button, originalLabel);
            });
    }

    function startPolling(button, originalLabel, activeJobId, startedAtMs) {
        var existing = getGlobalPoller();
        if (existing && existing.intervalId) {
            clearInterval(existing.intervalId);
        }

        button.disabled = true;
        setButtonText(button, "Indexing...");

        var runTick = function () {
            fetchStatus()
                .then(function (status) {
                    // 1) Ignore stale terminal snapshot while a different job is active.
                    if (status.complete && status.jobId && activeJobId && status.jobId !== activeJobId) {
                        renderProgress(button, {
                            message: "Waiting for full index job to start...",
                            pagesIndexed: 0,
                            assetsIndexed: 0,
                            currentBatchNumber: 0,
                            elapsedMs: 0,
                            lastIndexedPath: "",
                            completedAt: 0
                        });
                        return;
                    }

                    // 2) Terminal: render and stop.
                    if (status.complete) {
                        stopPolling();
                        clearProgress(button);
                        renderTerminalResult(button, status);
                        button.disabled = false;
                        setButtonText(button, originalLabel);
                        return;
                    }

                    // 3) Mismatch check while RUNNING only.
                    if (
                        status.running &&
                        status.jobId &&
                        activeJobId &&
                        status.jobId !== activeJobId
                    ) {
                        stopPolling();
                        clearProgress(button);
                        renderResult(
                            button,
                            "error",
                            "Full index status does not match the started job.",
                            "activeJobId=" + activeJobId + " | status.jobId=" + status.jobId,
                            "Failure"
                        );
                        button.disabled = false;
                        setButtonText(button, originalLabel);
                        return;
                    }

                    // 4) Still running.
                    renderProgress(button, status);
                })
                .catch(function (err) {
                    console.error(LOG, "Status polling error:", err);
                    stopPolling();
                    clearProgress(button);
                    renderResult(button, "error", "Unable to fetch full index status: " + err.message, "", "Failure");
                    button.disabled = false;
                    setButtonText(button, originalLabel);
                });
        };

        runTick();
        var intervalId = setInterval(runTick, POLL_INTERVAL_MS);
        setPoller(intervalId, activeJobId, startedAtMs);
    }

    function resumePollingIfRunning() {
        var button = findRunButton();
        if (!button) {
            return;
        }
        fetchStatus()
            .then(function (status) {
                if (!status.running) {
                    return;
                }
                renderProgress(button, status);
                startPolling(button, "Run full indexing", status.jobId || "", status.startedAt || Date.now());
            })
            .catch(function (err) {
                console.warn(LOG, "Could not resume full index status polling:", err);
            });
    }

    var delegatedBound = false;

    function attachDelegatedClickOnce() {
        if (delegatedBound) {
            return;
        }
        delegatedBound = true;

        document.addEventListener("click", function (e) {
            var button = resolveRunButton(e.target);
            if (!button) {
                return;
            }
            e.preventDefault();
            e.stopPropagation();
            handleRunFullIndex(button);
        }, false);
    }

    document.addEventListener("foundation-contentloaded", function () {
        attachDelegatedClickOnce();
        resumePollingIfRunning();
    });

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            attachDelegatedClickOnce();
            resumePollingIfRunning();
        });
    } else {
        attachDelegatedClickOnce();
        resumePollingIfRunning();
    }
})(document);
