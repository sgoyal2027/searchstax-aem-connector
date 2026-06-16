(function () {
    "use strict";

    function initSearch(root) {
        if (!root || root.dataset.initialized === "true") {
            return;
        }
        root.dataset.initialized = "true";

        var endpoint = root.getAttribute("data-select-endpoint");
        var token = root.getAttribute("data-select-token");
        var profile = root.getAttribute("data-search-profile");
        var pageSize = parseInt(root.getAttribute("data-results-per-page") || "10", 10);
        var form = root.querySelector(".searchstax-search__form");
        var results = root.querySelector(".searchstax-search__results");

        if (!form || !results || !endpoint) {
            return;
        }

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            var input = form.querySelector(".searchstax-search__input");
            var query = input ? input.value.trim() : "";
            if (!query) {
                results.innerHTML = "";
                return;
            }

            var url = endpoint;
            url += (url.indexOf("?") >= 0 ? "&" : "?") + "q=" + encodeURIComponent(query);
            url += "&rows=" + pageSize;
            if (profile) {
                url += "&Model=" + encodeURIComponent(profile);
            }

            results.innerHTML = "<p>Searching…</p>";

            fetch(url, {
                headers: {
                    Authorization: "Token " + token,
                    Accept: "application/json"
                }
            })
                .then(function (response) {
                    return response.json();
                })
                .then(function (data) {
                    renderResults(results, data);
                })
                .catch(function () {
                    results.innerHTML = "<p>Search is temporarily unavailable.</p>";
                });
        });
    }

    function renderResults(container, data) {
        var docs = (data && data.response && data.response.docs) || [];
        if (!docs.length) {
            container.innerHTML = "<p>No results found.</p>";
            return;
        }

        container.innerHTML = docs
            .map(function (doc) {
                var title = doc.title || doc.title_txt_en || doc.id || "Result";
                var url = doc.url || doc.path || "#";
                var snippet = doc.description || doc.content || "";
                return (
                    '<div class="searchstax-search__hit">' +
                    '<a href="' + escapeHtml(url) + '">' + escapeHtml(String(title)) + "</a>" +
                    (snippet ? "<p>" + escapeHtml(String(snippet).substring(0, 200)) + "</p>" : "") +
                    "</div>"
                );
            })
            .join("");
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".searchstax-search").forEach(initSearch);
    });
})();
