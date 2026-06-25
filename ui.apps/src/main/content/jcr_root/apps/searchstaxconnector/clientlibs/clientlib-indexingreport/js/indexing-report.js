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
    var fullReindexRunning = false;
    var REFRESH_MS_RUNNING = 10000;

    function getReportTbody(tableId) {
        var root = document.querySelector(tableId);
        if (!root) {
            return null;
        }

        root.querySelectorAll(":scope > tbody").forEach(function (orphan) {
            orphan.remove();
        });

        var table = root.tagName === "TABLE" ? root : root.querySelector("table");
        if (!table) {
            var coralTable = root.querySelector("coral-table");
            if (coralTable) {
                table = coralTable.querySelector("table");
            }
        }
        if (!table) {
            table = document.createElement("table");
            table.className = "coral-Table";
            root.appendChild(table);
        }

        suppressTableEmptyState(root);

        var thead = table.querySelector("thead");
        if (!thead) {
            thead = document.createElement("thead");
            thead.className = "coral-Table-head";
            table.insertBefore(thead, table.firstChild);
        }

        ensureTableHeader(tableId, thead);

        var tbody = table.querySelector("tbody");
        if (!tbody) {
            tbody = document.createElement("tbody");
            tbody.className = "coral-Table-body";
            table.appendChild(tbody);
        }

        return tbody;
    }

    function suppressTableEmptyState(root) {
        if (!root) {
            return;
        }
        root.querySelectorAll(
            ".foundation-layout-emptytext, .coral-Table-message, ._coral-Table-placeholder, [handle='placeholder']"
        ).forEach(function (element) {
            element.style.display = "none";
        });
    }

    function ensureTableHeader(tableId, thead) {
        if (!thead || thead.children.length > 0) {
            return;
        }

        var headers = ["Time", "Path", "Action", "Status", "Message"];
        var headerRow = document.createElement("tr");
        headers.forEach(function (title) {
            var th = document.createElement("th");
            th.className = "coral-Table-headerCell";
            th.textContent = title;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
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

    function loadFullReindexReport(statusFilter) {
        var requestId = ++activeRequestId;
        var url = REPORT_URL + "?limit=500&type=full";

        if (statusFilter && statusFilter !== "ALL") {
            url += "&status=" + encodeURIComponent(statusFilter);
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
        $.ajax({ url: FULL_INDEX_STATUS_URL, method: "GET", dataType: "json" })
            .done(function (data) {
                if (activeTab !== "full") {
                    return;
                }
                fullReindexRunning = !!(data && data.running);
                scheduleRefreshInterval();
            })
            .fail(function () {
                if (activeTab !== "full") {
                    return;
                }
                fullReindexRunning = false;
                scheduleRefreshInterval();
            });
    }

    function scheduleRefreshInterval() {
        if (!refreshTimer) {
            return;
        }
        clearInterval(refreshTimer);
        refreshTimer = setInterval(refreshReport, fullReindexRunning ? REFRESH_MS_RUNNING : REFRESH_MS);
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
                "<td>" + escapeHtml(formatIncrementalMessage(event.message || "")) + "</td>";
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
            var emptyMessage = "No full reindex events found for the selected filters.";
            if (fullReindexRunning) {
                emptyMessage =
                    "Full reindex is running. Success and failure rows appear here after the first batch is posted to SearchStax.";
            }
            appendEmptyRow(tbody, emptyMessage);
            return;
        }

        events.forEach(function (event) {
            var row = document.createElement("tr");
            row.innerHTML =
                "<td>" + escapeHtml(event.timestamp || "") + "</td>" +
                "<td>" + escapeHtml(event.path || "") + "</td>" +
                "<td>" + formatFullReindexAction() + "</td>" +
                "<td>" + formatStatus(event.status || "") + "</td>" +
                "<td>" + escapeHtml(formatFullReindexMessage(event)) + "</td>";
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

    function stripReportMessageTiming(message) {
        return String(message || "").replace(/\s*\(\d+\s*ms\)/gi, "").trim();
    }

    function formatMissingMandatoryFieldMessage(message) {
        if (!message || message.indexOf("MISSING_MANDATORY_FIELD:") !== 0) {
            return null;
        }

        var fieldMatch = message.match(/MISSING_MANDATORY_FIELD:\s*\S+\s*(\([^)]+\))/i);
        if (fieldMatch) {
            return "Missing mandatory field: " + fieldMatch[1];
        }

        var detailMatch = message.match(/MISSING_MANDATORY_FIELD:\s*(.+)/i);
        if (detailMatch) {
            return "Missing mandatory field: " + detailMatch[1].trim();
        }

        return "Missing mandatory field";
    }

    function formatIncrementalMessage(message) {
        if (!message) {
            return "";
        }

        var mandatoryFieldMessage = formatMissingMandatoryFieldMessage(message);
        if (mandatoryFieldMessage) {
            return mandatoryFieldMessage;
        }

        if (message.indexOf("HTTP ") >= 0) {
            return message;
        }

        var legacyMessages = {
            PERMANENT_FAILURE:
                "SearchStax rejected the index request and it will not be retried. Check API Configuration and the SearchStax response for HTTP status details.",
            DELETE_PERMANENT_FAILURE:
                "SearchStax rejected the delete request and it will not be retried. Check API Configuration and the SearchStax response for HTTP status details.",
            PLAN_LIMIT_EXCEEDED:
                "SearchStax plan document limit exceeded (HTTP 429).",
            MAX_RETRY_COUNT_EXHAUSTED:
                "Indexing failed after 5 retry attempts due to transient SearchStax or network errors.",
            MAX_RETRY_COUNT_REACHED:
                "Could not build the index document after 5 retry attempts.",
            DELETE_RETRY_EXHAUSTED:
                "Delete from search index failed after 5 retry attempts."
        };

        return legacyMessages[message] || message;
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

    function formatFullReindexMessage(event) {
        var message = (event && event.message) || "";
        if (!message) {
            return "";
        }

        var mandatoryFieldMessage = formatMissingMandatoryFieldMessage(message);
        if (mandatoryFieldMessage) {
            return mandatoryFieldMessage;
        }

        var formatted = message;
        if (formatted.indexOf("Successfully ") !== 0
                && formatted.indexOf("Full reindex") !== 0
                && formatted.indexOf("Could not") !== 0
                && formatted.indexOf("Document exceeds") !== 0
                && formatted.indexOf("Path failed") !== 0) {

            if (formatted.indexOf("HTTP ") === 0) {
                formatted = "Full reindex request failed (" + formatted + ")";
            } else if (formatted.indexOf("Indexed in batch ") === 0) {
                formatted = formatted.replace(
                    "Indexed in batch ",
                    "Successfully posted to SearchStax in full reindex batch ");
            } else if (formatted.indexOf("(retries:") >= 0) {
                formatted = "Full reindex batch failed to post to SearchStax " + formatted;
            }
        }

        return stripReportMessageTiming(formatted);
    }

    function formatFullReindexAction() {
        return "<span class=\"searchstax-action\">Full Index</span>";
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function refreshReport() {
        activeTab = resolveActiveTab();
        if (activeTab === "full") {
            loadFullReindexReport(getSelectedValue("#searchstax-full-reindex-status-filter"));
            return;
        }
        loadIncrementalReport(
            getSelectedValue("#searchstax-indexing-report-status-filter"),
            getSelectedValue("#searchstax-indexing-report-action-filter"));
    }

    function bindTabIdentifiers(tabView) {
        if (!tabView || tabView.dataset.searchstaxTabIdsBound === "true") {
            return;
        }
        tabView.dataset.searchstaxTabIdsBound = "true";

        var tabs = tabView.querySelectorAll("coral-tab");
        tabs.forEach(function (tab, index) {
            tab.setAttribute("data-searchstax-report-tab", index === 0 ? "incremental" : "full");
        });
    }

    function resolveActiveTab() {
        var tabs = document.querySelector("#searchstax-indexing-report-tabs");
        if (!tabs) {
            return "incremental";
        }

        var tabView = tabs.tagName === "CORAL-TABVIEW" ? tabs : tabs.querySelector("coral-tabview");
        if (tabView) {
            if (tabView.selectedItem) {
                var tabKey = tabView.selectedItem.getAttribute("data-searchstax-report-tab");
                if (tabKey) {
                    return tabKey;
                }

                var label = "";
                if (tabView.selectedItem.label && tabView.selectedItem.label.textContent) {
                    label = tabView.selectedItem.label.textContent;
                } else {
                    label = tabView.selectedItem.textContent || "";
                }
                if (label.toLowerCase().indexOf("full") >= 0) {
                    return "full";
                }

                var panelId = (tabView.selectedItem.getAttribute("aria-controls") || "").toLowerCase();
                if (panelId.indexOf("full") >= 0) {
                    return "full";
                }
            }

            if (typeof tabView.selectedIndex === "number" && tabView.selectedIndex > 0) {
                return "full";
            }

            var selectedTab = tabView.querySelector("coral-tab[selected]");
            if (selectedTab) {
                var selectedKey = selectedTab.getAttribute("data-searchstax-report-tab");
                if (selectedKey) {
                    return selectedKey;
                }
            }
        }

        var panels = tabs.querySelectorAll("coral-panel");
        for (var i = 0; i < panels.length; i++) {
            if (panels[i].selected) {
                return i === 0 ? "incremental" : "full";
            }
        }

        return "incremental";
    }

    function redirectToStartPage() {
        window.location.href = "/aem/start.html";
    }

    function setWizardButtonLabel(button, text) {
        var label = button.querySelector("coral-button-label");
        if (label) {
            label.textContent = text;
            return;
        }
        if (button.label && button.label.textContent !== undefined) {
            button.label.textContent = text;
            return;
        }
        button.textContent = text;
    }

    function styleWizardCloseButton(button) {
        button.setAttribute("is", "coral-anchorbutton");
        button.setAttribute("variant", "secondary");
        button.setAttribute("role", "button");
        button.className = "coral-Button coral-Button--secondary searchstax-indexing-report-close-btn";
    }

    function createWizardCloseButton() {
        var closeBtn = document.createElement("a");
        closeBtn.id = "searchstax-indexing-report-close";
        closeBtn.href = "#";
        styleWizardCloseButton(closeBtn);
        closeBtn.textContent = "Close";

        closeBtn.addEventListener("click", function (event) {
            event.preventDefault();
            redirectToStartPage();
        });

        if (window.Coral && Coral.commons && typeof Coral.commons.ready === "function") {
            Coral.commons.ready(closeBtn, function () {
                setWizardButtonLabel(closeBtn, "Close");
            });
        }

        return closeBtn;
    }

    function ensureReportHeader() {
        var existingRow = document.querySelector(".searchstax-indexing-report-header-row");
        if (existingRow) {
            var existingBtn = existingRow.querySelector("#searchstax-indexing-report-close");
            if (existingBtn) {
                if (existingBtn.tagName !== "A") {
                    var replacement = createWizardCloseButton();
                    existingBtn.parentNode.replaceChild(replacement, existingBtn);
                } else {
                    styleWizardCloseButton(existingBtn);
                    setWizardButtonLabel(existingBtn, "Close");
                }
            }
            return;
        }

        var tabs = document.querySelector("#searchstax-indexing-report-tabs");
        if (!tabs || !tabs.parentNode) {
            return;
        }

        var oldHeader = document.querySelector(".searchstax-indexing-report-header");
        if (oldHeader) {
            oldHeader.remove();
        }

        var row = document.createElement("div");
        row.className = "searchstax-indexing-report-header-row";

        var title = document.createElement("div");
        title.className = "searchstax-indexing-report-header-title";
        title.innerHTML = "<h2 class=\"coral-Heading coral-Heading--2\">Indexing Report</h2>";

        var actions = document.createElement("div");
        actions.className = "searchstax-indexing-report-header-actions";

        var closeBtn = createWizardCloseButton();

        actions.appendChild(closeBtn);
        row.appendChild(title);
        row.appendChild(actions);
        tabs.parentNode.insertBefore(row, tabs);
        tabs.parentNode.classList.add("searchstax-indexing-report-body");
    }

    function ensureReportSpacing() {
        var tabs = document.querySelector("#searchstax-indexing-report-tabs");
        if (tabs) {
            tabs.querySelectorAll("coral-panel, .coral-Panel, .coral-TabPanel").forEach(function (panel) {
                panel.classList.add("searchstax-indexing-report-tab-panel");
            });
        }

        document.querySelectorAll(".searchstax-indexing-report-description").forEach(function (el) {
            el.classList.add("searchstax-indexing-report-has-spacing");
        });

        document.querySelectorAll(".searchstax-indexing-report-filters").forEach(function (el) {
            el.classList.add("searchstax-indexing-report-has-spacing");
        });

        ["#searchstax-indexing-report-action-filter", "#searchstax-full-reindex-status-filter"].forEach(function (selector) {
            var filter = document.querySelector(selector);
            if (filter) {
                var filtersRow = filter.closest(".searchstax-indexing-report-filters")
                    || filter.closest(".coral-Form-fieldwrapper")
                    || filter.parentElement;
                if (filtersRow) {
                    filtersRow.classList.add("searchstax-indexing-report-has-spacing");
                }
            }
        });

        ["#searchstax-indexing-report-table", "#searchstax-full-reindex-report-table"].forEach(function (selector) {
            var table = document.querySelector(selector);
            if (table) {
                table.classList.add("searchstax-indexing-report-table-spaced");
            }
        });
    }

    function ensureReportContentInsets() {
        var tabs = document.querySelector("#searchstax-indexing-report-tabs");
        if (!tabs || !tabs.parentNode) {
            return;
        }

        tabs.parentNode.classList.add("searchstax-indexing-report-body");
    }

    function bindCloseButton() {
        if (document.body.dataset.searchstaxIndexingReportCloseBound === "true") {
            return;
        }
        document.body.dataset.searchstaxIndexingReportCloseBound = "true";

        $(document).on("click", "#searchstax-indexing-report-close", function (event) {
            event.preventDefault();
            redirectToStartPage();
        });
    }

    function bindTabs() {
        var tabs = document.querySelector("#searchstax-indexing-report-tabs");
        if (!tabs || tabs.dataset.searchstaxTabsBound === "true") {
            return;
        }
        tabs.dataset.searchstaxTabsBound = "true";

        var attachTabHandler = function (tabView) {
            bindTabIdentifiers(tabView);
            tabView.addEventListener("coral-tabview:change", function () {
                activeTab = resolveActiveTab();
                window.setTimeout(refreshReport, 0);
            });
        };

        if (window.Coral && Coral.commons && typeof Coral.commons.ready === "function") {
            Coral.commons.ready(tabs, function (tabView) {
                ensureReportSpacing();
                var element = tabView.tagName === "CORAL-TABVIEW" ? tabView : tabView.querySelector("coral-tabview");
                if (element) {
                    bindTabIdentifiers(element);
                    attachTabHandler(element);
                    activeTab = resolveActiveTab();
                }
            });
        } else {
            ensureReportSpacing();
            var tabView = tabs.querySelector("coral-tabview");
            if (tabView) {
                attachTabHandler(tabView);
            }
        }
    }

    function initIndexingReport() {
        ensureReportHeader();
        ensureReportContentInsets();
        ensureReportSpacing();
        bindCloseButton();

        if (!document.querySelector("#searchstax-indexing-report-tabs")) {
            return;
        }

        if (!filtersInitialized) {
            filtersInitialized = true;
            bindCloseButton();
            bindTabs();
            bindFilter("#searchstax-indexing-report-action-filter");
            bindFilter("#searchstax-indexing-report-status-filter");
            bindFilter("#searchstax-full-reindex-status-filter");
            $(document).on(
                "foundation-field-change",
                "#searchstax-indexing-report-action-filter, #searchstax-indexing-report-status-filter, #searchstax-full-reindex-status-filter",
                refreshReport);
            refreshTimer = setInterval(refreshReport, REFRESH_MS);
        }

        activeTab = resolveActiveTab();
        if (activeTab === "full") {
            loadFullReindexSummary();
        }
        refreshReport();
    }

    $(document).on("foundation-contentloaded", initIndexingReport);
})(document, Granite.$);
