(function (document, $) {
    "use strict";

    var REPORT_URL = "/bin/searchstaxconnector/wizard/indexing-report";
    var REPROCESS_URL = "/bin/searchstaxconnector/wizard/indexing-report-reprocess";
    var FULL_INDEX_STATUS_URL = "/bin/searchstaxconnector/wizard/fullindex-status";
    var REFRESH_MS = 60000;

    var refreshTimer = null;
    var filtersInitialized = false;
    var activeRequestId = 0;
    var activeTab = "incremental";

    function getReportTbody(tableId) {
        var root = document.querySelector(tableId);
        if (!root) {
            return null;
        }

        root.querySelectorAll(":scope > tbody").forEach(function (orphan) {
            orphan.remove();
        });

        return root.querySelector("table tbody") || root.querySelector("tbody");
    }

    function loadIncrementalReport(statusFilter, actionFilter) {
        var requestId = ++activeRequestId;
        var url = REPORT_URL + "?limit=500&type=incremental";
        if (statusFilter && statusFilter !== "ALL") {
            url += "&status=" + encodeURIComponent(statusFilter);
        }
        if (actionFilter && actionFilter !== "ALL") {
            url += "&action=" + encodeURIComponent(actionFilter);
        }
        url += "&excludeQueued=true";

        $.ajax({ url: url, method: "GET", dataType: "json" })
            .done(function (data) {
                if (requestId !== activeRequestId || activeTab !== "incremental") {
                    return;
                }
                renderIncrementalRows(data && data.events ? data.events : []);
            })
            .fail(function () {
                if (requestId !== activeRequestId || activeTab !== "incremental") {
                    return;
                }
                renderIncrementalRows([]);
            });
    }

    function loadFullReindexReport(statusFilter, failureKindFilter) {
        var requestId = ++activeRequestId;
        var url = REPORT_URL + "?limit=500&type=full";
        if (statusFilter && statusFilter !== "ALL") {
            url += "&status=" + encodeURIComponent(statusFilter);
        }
        if (failureKindFilter && failureKindFilter !== "ALL") {
            url += "&failureKind=" + encodeURIComponent(failureKindFilter);
        }

        $.ajax({ url: url, method: "GET", dataType: "json" })
            .done(function (data) {
                if (requestId !== activeRequestId || activeTab !== "full") {
                    return;
                }
                renderFullReindexRows(data && data.events ? data.events : []);
            })
            .fail(function () {
                if (requestId !== activeRequestId || activeTab !== "full") {
                    return;
                }
                renderFullReindexRows([]);
            });

        loadFullReindexSummary();
    }

    function loadFullReindexSummary() {
        var summary = document.querySelector("#searchstax-full-reindex-run-summary");
        if (!summary) {
            return;
        }

        $.ajax({ url: FULL_INDEX_STATUS_URL, method: "GET", dataType: "json" })
            .done(function (data) {
                if (activeTab !== "full") {
                    return;
                }
                renderFullReindexSummary(summary, data || {});
            })
            .fail(function () {
                if (activeTab !== "full") {
                    return;
                }
                summary.innerHTML = "";
            });
    }

    function renderFullReindexSummary(container, data) {
        var state = data.state || "IDLE";
        var running = !!data.running;
        var successCount = data.successCount != null ? data.successCount : 0;
        var failureCount = data.failureCount != null ? data.failureCount : 0;
        var totalAttempted = data.totalAttempted != null ? data.totalAttempted : 0;
        var message = data.message || "";

        if (state === "IDLE" && !running && totalAttempted === 0) {
            container.innerHTML = "";
            return;
        }

        container.innerHTML =
            "<div class='searchstax-full-reindex-summary-panel'>" +
                "<div class='searchstax-full-reindex-summary-title'>Last full reindex run</div>" +
                "<div class='searchstax-full-reindex-summary-stats'>" +
                    "<span><strong>State:</strong> " + escapeHtml(state) + "</span>" +
                    "<span><strong>Indexed:</strong> " + escapeHtml(String(successCount)) + "</span>" +
                    "<span><strong>Failed paths:</strong> " + escapeHtml(String(failureCount)) + "</span>" +
                    "<span><strong>Attempted:</strong> " + escapeHtml(String(totalAttempted)) + "</span>" +
                "</div>" +
                (message ? "<div class='searchstax-full-reindex-summary-message'>" + escapeHtml(message) + "</div>" : "") +
            "</div>";
    }

    function renderIncrementalRows(events) {
        var tbody = getReportTbody("#searchstax-indexing-report-table");
        if (!tbody) {
            return;
        }

        tbody.innerHTML = "";

        if (!events.length) {
            appendEmptyRow(tbody, "No incremental indexing events found for the selected filters.");
            return;
        }

        events.forEach(function (event) {
            var row = document.createElement("tr");
            row.innerHTML =
                "<td>" + escapeHtml(event.timestamp || "") + "</td>" +
                "<td>" + escapeHtml(event.path || "") + "</td>" +
                "<td>" + formatIncrementalAction(event.action || "") + "</td>" +
                "<td>" + formatStatus(event.status || "") + "</td>" +
                "<td>" + escapeHtml(event.message || "") + "</td>";
            tbody.appendChild(row);

            if (event.status === "FAILURE") {
                row.style.cursor = "pointer";
                row.title = "Double-click to reprocess";
                row.addEventListener("dblclick", function () {
                    reprocess(event.path);
                });
            }
        });
    }

    function renderFullReindexRows(events) {
        var tbody = getReportTbody("#searchstax-full-reindex-report-table");
        if (!tbody) {
            return;
        }

        tbody.innerHTML = "";

        if (!events.length) {
            appendEmptyRow(tbody, "No full reindex events found for the selected filters.");
            return;
        }

        events.forEach(function (event) {
            var row = document.createElement("tr");
            row.innerHTML =
                "<td>" + escapeHtml(event.timestamp || "") + "</td>" +
                "<td>" + escapeHtml(event.path || "") + "</td>" +
                "<td>" + formatFullReindexType(event) + "</td>" +
                "<td>" + formatStatus(event.status || "") + "</td>" +
                "<td>" + escapeHtml(event.message || "") + "</td>";
            tbody.appendChild(row);
        });
    }

    function appendEmptyRow(tbody, message) {
        var emptyRow = document.createElement("tr");
        emptyRow.className = "searchstax-indexing-report-empty";
        emptyRow.innerHTML = "<td colspan=\"5\">" + escapeHtml(message) + "</td>";
        tbody.appendChild(emptyRow);
    }

    function reprocess(path) {
        if (!path || !window.confirm("Reprocess failed item?\n" + path)) {
            return;
        }
        $.ajax({
            url: REPROCESS_URL,
            method: "POST",
            data: { path: path }
        }).always(function () {
            refreshReport();
        });
    }

    function resolveCoralSelect(selector) {
        var wrapper = document.querySelector(selector);
        if (!wrapper) {
            return null;
        }
        if (wrapper.tagName === "CORAL-SELECT") {
            return wrapper;
        }
        return wrapper.querySelector("coral-select");
    }

    function getSelectedValue(selector) {
        var coralSelect = resolveCoralSelect(selector);
        if (!coralSelect) {
            return "ALL";
        }

        var value = coralSelect.value;
        if (!value && coralSelect.selectedItem) {
            value = coralSelect.selectedItem.value;
        }
        if (value) {
            return value;
        }

        var wrapper = document.querySelector(selector);
        if (wrapper) {
            var field = $(wrapper).adaptTo("foundation-field");
            if (field && typeof field.getValue === "function") {
                var fieldValue = field.getValue();
                if (Array.isArray(fieldValue)) {
                    return fieldValue.length ? fieldValue[0] : "ALL";
                }
                return fieldValue || "ALL";
            }
        }

        return "ALL";
    }

    function bindFilter(selectSelector) {
        var coralSelect = resolveCoralSelect(selectSelector);
        if (!coralSelect || coralSelect.dataset.searchstaxFilterBound === "true") {
            return;
        }
        coralSelect.dataset.searchstaxFilterBound = "true";

        var attachChangeHandler = function (select) {
            if (typeof select.on === "function") {
                select.on("change", refreshReport);
            } else {
                select.addEventListener("change", refreshReport);
            }
        };

        if (window.Coral && Coral.commons && typeof Coral.commons.ready === "function") {
            Coral.commons.ready(coralSelect, function () {
                attachChangeHandler(coralSelect);
            });
        } else {
            attachChangeHandler(coralSelect);
        }
    }

    function formatStatus(status) {
        var normalized = escapeHtml(status || "");
        var cssClass = "searchstax-badge";
        if (status === "SUCCESS") {
            cssClass += " searchstax-badge--success";
        } else if (status === "FAILURE") {
            cssClass += " searchstax-badge--failure";
        } else if (status === "SKIPPED") {
            cssClass += " searchstax-badge--skipped";
        }
        return "<span class=\"" + cssClass + "\">" + normalized + "</span>";
    }

    function formatIncrementalAction(action) {
        var label = action || "";
        if (action === "ACTIVATE") {
            label = "Publish";
        } else if (action === "DEACTIVATE") {
            label = "Unpublish";
        }
        return "<span class=\"searchstax-action\">" + escapeHtml(label) + "</span>";
    }

    function formatFullReindexType(event) {
        var label = "Full Reindex";
        if (event && event.status === "SUCCESS") {
            label = "Batch success";
        } else if (event && event.failureKind === "BATCH") {
            label = "Batch failure";
        } else if (event && event.failureKind === "PATH") {
            label = "Path failure";
        }
        return "<span class=\"searchstax-action\">" + escapeHtml(label) + "</span>";
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function refreshReport() {
        if (activeTab === "full") {
            loadFullReindexReport(
                getSelectedValue("#searchstax-full-reindex-status-filter"),
                getSelectedValue("#searchstax-full-reindex-failure-kind-filter"));
            return;
        }
        loadIncrementalReport(
            getSelectedValue("#searchstax-indexing-report-status-filter"),
            getSelectedValue("#searchstax-indexing-report-action-filter"));
    }

    function resolveActiveTab() {
        var tabs = document.querySelector("#searchstax-indexing-report-tabs");
        if (!tabs) {
            return "incremental";
        }
        var tabView = tabs.tagName === "CORAL-TABVIEW" ? tabs : tabs.querySelector("coral-tabview");
        if (!tabView || !tabView.selectedItem) {
            return "incremental";
        }
        var panelId = tabView.selectedItem.getAttribute("aria-controls") || "";
        if (panelId.indexOf("fullReindexTab") >= 0 || panelId.indexOf("fullreindex") >= 0) {
            return "full";
        }
        var label = (tabView.selectedItem.label && tabView.selectedItem.label.textContent) || "";
        if (label.toLowerCase().indexOf("full reindex") >= 0) {
            return "full";
        }
        return "incremental";
    }

    function bindTabs() {
        var tabs = document.querySelector("#searchstax-indexing-report-tabs");
        if (!tabs || tabs.dataset.searchstaxTabsBound === "true") {
            return;
        }
        tabs.dataset.searchstaxTabsBound = "true";

        var attachTabHandler = function (tabView) {
            tabView.addEventListener("coral-tabview:change", function () {
                activeTab = resolveActiveTab();
                refreshReport();
            });
        };

        if (window.Coral && Coral.commons && typeof Coral.commons.ready === "function") {
            Coral.commons.ready(tabs, function (tabView) {
                var element = tabView.tagName === "CORAL-TABVIEW" ? tabView : tabView.querySelector("coral-tabview");
                if (element) {
                    attachTabHandler(element);
                }
            });
        } else {
            var tabView = tabs.querySelector("coral-tabview");
            if (tabView) {
                attachTabHandler(tabView);
            }
        }
    }

    function initIndexingReport() {
        if (!document.querySelector("#searchstax-indexing-report-tabs")) {
            return;
        }

        if (!filtersInitialized) {
            filtersInitialized = true;
            bindTabs();
            bindFilter("#searchstax-indexing-report-action-filter");
            bindFilter("#searchstax-indexing-report-status-filter");
            bindFilter("#searchstax-full-reindex-status-filter");
            bindFilter("#searchstax-full-reindex-failure-kind-filter");
            $(document).on(
                "foundation-field-change",
                "#searchstax-indexing-report-action-filter, #searchstax-indexing-report-status-filter, #searchstax-full-reindex-status-filter, #searchstax-full-reindex-failure-kind-filter",
                refreshReport);
            refreshTimer = setInterval(refreshReport, REFRESH_MS);
        }

        activeTab = resolveActiveTab();
        refreshReport();
    }

    $(document).on("foundation-contentloaded", initIndexingReport);
})(document, Granite.$);
