(function (document, $) {
    "use strict";

    var REPORT_URL = "/bin/searchstaxconnector/wizard/indexing-report";
    var REPROCESS_URL = "/bin/searchstaxconnector/wizard/indexing-report-reprocess";
    var REFRESH_MS = 60000;

    function loadReport(statusFilter) {
        var url = REPORT_URL + "?limit=500";
        if (statusFilter && statusFilter !== "ALL") {
            url += "&status=" + encodeURIComponent(statusFilter);
        }

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
                "<td>" + escapeHtml(event.action || "") + "</td>" +
                "<td>" + escapeHtml(event.status || "") + "</td>" +
                "<td>" + escapeHtml(String(event.duration != null ? event.duration : "")) + "</td>" +
                "<td>" + escapeHtml(event.correlationId || "") + "</td>" +
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
            loadReport("ALL");
        });
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
        loadReport("ALL");
        setInterval(function () {
            loadReport("ALL");
        }, REFRESH_MS);
    });
})(document, Granite.$);
