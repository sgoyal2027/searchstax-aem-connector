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
            loadReport(getSelectedValue("#searchstax-indexing-report-status-filter"), getSelectedValue("#searchstax-indexing-report-action-filter"));
        });
    }

    function getSelectedValue(selector) {
        var input = document.querySelector(selector);
        if (!input) {
            return "ALL";
        }
        return input.value || "ALL";
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

    $(document).on("foundation-contentloaded", function () {
        if (!document.querySelector("#searchstax-indexing-report-table")) {
            return;
        }
        var actionFilter = document.querySelector("#searchstax-indexing-report-action-filter");
        var statusFilter = document.querySelector("#searchstax-indexing-report-status-filter");
        var refresh = function () {
            loadReport(getSelectedValue("#searchstax-indexing-report-status-filter"), getSelectedValue("#searchstax-indexing-report-action-filter"));
        };

        if (actionFilter) {
            actionFilter.addEventListener("change", refresh);
        }
        if (statusFilter) {
            statusFilter.addEventListener("change", refresh);
        }

        refresh();
        setInterval(function () {
            refresh();
        }, REFRESH_MS);
    });
})(document, Granite.$);
