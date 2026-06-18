(function (document, $) {
    "use strict";

    var REPORT_URL = "/bin/searchstaxconnector/wizard/indexing-report";
    var REPROCESS_URL = "/bin/searchstaxconnector/wizard/indexing-report-reprocess";
    var REFRESH_MS = 60000;

    function loadReport(statusFilter, actionFilter) {
        var url = REPORT_URL + "?limit=500";
        if (statusFilter && statusFilter !== "ALL") {
            url += "&status=" + encodeURIComponent(statusFilter);
        }
        if (actionFilter && actionFilter !== "ALL") {
            url += "&action=" + encodeURIComponent(actionFilter);
        }
        url += "&excludeQueued=true";

        $.ajax({ url: url, method: "GET", dataType: "json" })
            .done(function (data) {
                renderRows(data && data.events ? data.events : []);
            })
            .fail(function () {
                renderRows([]);
            });
    }

    function renderRows(events) {
        var table = document.querySelector("#searchstax-indexing-report-table");
        if (!table) {
            return;
        }

        var tbody = table.querySelector("tbody");
        if (!tbody) {
            tbody = document.createElement("tbody");
            table.appendChild(tbody);
        }

        tbody.innerHTML = "";

        if (!events.length) {
            var emptyRow = document.createElement("tr");
            emptyRow.className = "searchstax-indexing-report-empty";
            emptyRow.innerHTML = "<td colspan=\"6\">No indexing events found for the selected filters.</td>";
            tbody.appendChild(emptyRow);
            return;
        }

        events.forEach(function (event) {
            var row = document.createElement("tr");
            row.innerHTML =
                "<td>" + escapeHtml(event.timestamp || "") + "</td>" +
                "<td>" + escapeHtml(event.path || "") + "</td>" +
                "<td>" + formatAction(event.action || "") + "</td>" +
                "<td>" + formatStatus(event.status || "") + "</td>" +
                "<td>" + escapeHtml(String(event.duration != null ? event.duration : "")) + "</td>" +
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

    function formatAction(action) {
        return "<span class=\"searchstax-action\">" + escapeHtml(action || "") + "</span>";
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function refreshReport() {
        loadReport(
            getSelectedValue("#searchstax-indexing-report-status-filter"),
            getSelectedValue("#searchstax-indexing-report-action-filter"));
    }

    var refreshTimer = null;
    var filtersInitialized = false;

    function initIndexingReport() {
        if (!document.querySelector("#searchstax-indexing-report-table")) {
            return;
        }

        if (!filtersInitialized) {
            filtersInitialized = true;
            bindFilter("#searchstax-indexing-report-action-filter");
            bindFilter("#searchstax-indexing-report-status-filter");
            $(document).on(
                "foundation-field-change",
                "#searchstax-indexing-report-action-filter, #searchstax-indexing-report-status-filter",
                refreshReport);
            refreshTimer = setInterval(refreshReport, REFRESH_MS);
        }

        refreshReport();
    }

    $(document).on("foundation-contentloaded", initIndexingReport);
    document.addEventListener("foundation-contentloaded", initIndexingReport);
})(document, Granite.$);
