package com.searchstax.aem.connector.core.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Build-time smoke checks for AEM 6.5 / AMS / Cloud cross-platform packaging.
 */
class ConnectorCrossPlatformSmokeTest {

    private static final Path REPO_ROOT = Paths.get(System.getProperty("user.dir")).getParent();

    private static final Set<String> REQUIRED_WIZARD_SERVLET_PATHS = Set.of(
            "/bin/searchstaxconnector/wizard/initial-setup-load",
            "/bin/searchstaxconnector/wizard/initial-setup-config",
            "/bin/searchstaxconnector/wizard/api-config-load",
            "/bin/searchstaxconnector/wizard/api-config-save",
            "/bin/searchstaxconnector/wizard/metadata-field-mappings-load",
            "/bin/searchstaxconnector/wizard/language-mappings-load",
            "/bin/searchstaxconnector/wizard/full-index-load",
            "/bin/searchstaxconnector/wizard/fullindex-config-save",
            "/bin/searchstaxconnector/wizard/indexing-report",
            "/bin/searchstaxconnector/wizard/site-application-mappings-load");

    private static final Set<String> REQUIRED_REPOINIT_PATHS = Set.of(
            "/conf/searchstaxconnector/settings/apiconfig",
            "/var/searchstaxconnector/incremental-index/pending",
            "/var/searchstaxconnector/incremental-index/audit",
            "/var/searchstaxconnector/incremental-index/failed");

    @Test
    void containerPackage_containsCoreBundleAndSubPackages() throws Exception {
        final Path allZip = REPO_ROOT.resolve(
                "all/target/searchstax-aem-connector.all-1.0.0-SNAPSHOT.zip");
        Assumptions.assumeTrue(
                Files.exists(allZip),
                "Skip until all module is packaged — run cross-platform-verify after mvn install");

        try (ZipFile zip = new ZipFile(allZip.toFile())) {
            final boolean hasCoreJar = zip.stream()
                    .anyMatch(e -> e.getName().contains("searchstax-aem-connector.core")
                            && e.getName().endsWith(".jar"));
            final boolean hasUiApps = zip.stream()
                    .anyMatch(e -> e.getName().contains("searchstax-aem-connector.ui.apps")
                            && e.getName().endsWith(".zip"));
            final boolean hasUiConfig = zip.stream()
                    .anyMatch(e -> e.getName().contains("searchstax-aem-connector.ui.config")
                            && e.getName().endsWith(".zip"));

            assertTrue(hasCoreJar, "Container must embed core OSGi bundle");
            assertTrue(hasUiApps, "Container must embed ui.apps content package");
            assertTrue(hasUiConfig, "Container must embed ui.config content package");
        }
    }

    @Test
    void cloudServletAllowlist_includesAllWizardLoadEndpoints() throws Exception {
        final Path cfg = REPO_ROOT.resolve(
                "ui.config/src/main/content/jcr_root/apps/searchstaxconnector/osgiconfig/config/"
                        + "org.apache.sling.servlets.resolver.internal.SlingServletResolver.cfg.json");
        assertTrue(Files.exists(cfg), "Missing Cloud servlet resolver config: " + cfg);

        final String json = Files.readString(cfg);
        for (final String required : REQUIRED_WIZARD_SERVLET_PATHS) {
            assertTrue(
                    json.contains("\"" + required + "\""),
                    "Cloud allowlist missing wizard servlet: " + required);
        }
    }

    @Test
    void repoinit_declaresIncrementalAndConfigPaths() throws Exception {
        final Path repoinit = REPO_ROOT.resolve(
                "ui.config/src/main/content/jcr_root/apps/searchstaxconnector/osgiconfig/config/"
                        + "org.apache.sling.jcr.repoinit.RepositoryInitializer~searchstaxconnector.cfg.json");
        assertTrue(Files.exists(repoinit), "Missing repoinit: " + repoinit);

        final String content = Files.readString(repoinit);
        for (final String path : REQUIRED_REPOINIT_PATHS) {
            assertTrue(content.contains(path), "Repoinit must create path: " + path);
        }
    }

    @Test
    void coreBundle_containsIncrementalListenerClass() throws Exception {
        final Path listenerClass = REPO_ROOT.resolve(
                "core/target/classes/com/searchstax/aem/connector/core/listeners/PublishListener.class");
        final Path packagedJar = REPO_ROOT.resolve(
                "core/target/searchstax-aem-connector.core-1.0.0-SNAPSHOT.jar");

        if (Files.exists(packagedJar)) {
            try (ZipFile zip = new ZipFile(packagedJar.toFile())) {
                assertTrue(
                        zip.stream().anyMatch(e -> e.getName().contains("PublishListener.class")),
                        "Core bundle must contain incremental PublishListener");
            }
            return;
        }

        assertTrue(
                Files.exists(listenerClass),
                "Compile core module first — missing " + listenerClass);
    }

    @Test
    void uiAppsPackage_includesToolsNavAndSearchComponent() throws Exception {
        final Path uiAppsZip = REPO_ROOT.resolve(
                "ui.apps/target/searchstax-aem-connector.ui.apps-1.0.0-SNAPSHOT.zip");
        Assumptions.assumeTrue(
                Files.exists(uiAppsZip),
                "Skip until ui.apps is packaged — run cross-platform-verify after mvn install");

        try (ZipFile zip = new ZipFile(uiAppsZip.toFile())) {
            final boolean hasToolsNav = zip.stream()
                    .anyMatch(e -> e.getName().contains("nav/tools"));
            final boolean hasSearchComponent = zip.stream()
                    .anyMatch(e -> e.getName().contains("searchstax-search"));
            assertTrue(hasToolsNav, "ui.apps must ship Tools navigation overlay");
            assertTrue(hasSearchComponent, "ui.apps must ship searchstax-search component");
        }
    }

    @Test
    void coreBundle_doesNotImportPlatformTikaApi() throws Exception {
        final Path packagedJar = REPO_ROOT.resolve(
                "core/target/searchstax-aem-connector.core-1.0.0-SNAPSHOT.jar");
        Assumptions.assumeTrue(
                Files.exists(packagedJar),
                "Skip until core module is packaged — run cross-platform-verify after mvn install");

        try (ZipFile zip = new ZipFile(packagedJar.toFile())) {
            final ZipEntry manifestEntry = zip.getEntry("META-INF/MANIFEST.MF");
            assertTrue(manifestEntry != null, "Core bundle must contain MANIFEST.MF");
            final String mf = new String(zip.getInputStream(manifestEntry).readAllBytes());
            assertTrue(!mf.contains("org.apache.tika;"), "Core bundle must not import platform Tika API");
        }
    }
}
