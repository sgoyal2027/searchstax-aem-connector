package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.dto.SearchStaxUpdateOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchstaxClientServiceImplTest {

    @Test
    void buildRequestUrl_incrementalHardCommit() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update", SearchStaxUpdateOptions.incrementalDefault());

        assertTrue(url.contains("commit=true"));
    }

    @Test
    void buildRequestUrl_fullIndexCommitWithinAndHardCommit() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update",
                SearchStaxUpdateOptions.fullIndexBatch(true, 120000L));

        assertTrue(url.contains("commitWithin=120000"));
        assertTrue(url.contains("commit=true"));
    }

    @Test
    void buildRequestUrl_includesSearchProfileModelParam() {
        final String url = SearchstaxClientServiceImpl.buildRequestUrl(
                "https://search.example/update",
                SearchStaxUpdateOptions.routed(true, null, null, null, "my-profile"));

        assertTrue(url.contains("Model=my-profile"));
    }
}
