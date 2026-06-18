import { Searchstax } from "@searchstax-inc/searchstudio-ux-js";
import "./main.scss";

function makeId(length: number): string {
    let result = "";
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

document.addEventListener("DOMContentLoaded", () => {
    const root = document.querySelector<HTMLElement>(".cmp-searchstudio");
    if (!root) {
        return;
    }

    const searchstax = new Searchstax();

    searchstax.initialize({
        language: "en",
        searchURL: root.dataset.searchUrl || "",
        suggesterURL: root.dataset.suggesterUrl || "",
        searchAuth: root.dataset.searchAuth || "",
        trackApiKey: root.dataset.trackKey || "",
        analyticsBaseUrl: root.dataset.analyticsUrl || "",
        authType: "token" as const,
        sessionId: makeId(25),
    });

    searchstax.addSearchInputWidget("searchstudio-input-container", {});

    searchstax.addFacetsWidget("searchstudio-facets-container", {
        facetingType: "and",
        itemsPerPageDesktop: 3,
        itemsPerPageMobile: 99,
    });

    searchstax.addSearchResultsWidget("searchstudio-results-container", {
        renderMethod: "pagination",
    });

    searchstax.addPaginationWidget("searchstudio-pagination-container", {});
});
