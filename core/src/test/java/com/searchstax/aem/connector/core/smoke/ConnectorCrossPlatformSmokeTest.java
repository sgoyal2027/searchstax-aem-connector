package com.searchstax.aem.connector.core.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
            "/bin/searchstaxconnector/wizard/indexing-report");

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
    void uiAppsFilter_usesMergeForToolsNavOnly() throws Exception {
        final Path filter = REPO_ROOT.resolve("ui.apps/src/main/content/META-INF/vault/filter.xml");
        assertTrue(Files.exists(filter), "Missing ui.apps filter.xml");

        final String content = Files.readString(filter);
        assertTrue(
                content.contains("/apps/cq/core/content/nav/tools/Searchstax"),
                "Tools nav must target Searchstax entry only");
        assertTrue(content.contains("mode=\"merge\""), "ui.apps filters must use merge mode");
        assertTrue(
                !content.contains("root=\"/apps/cq/core/content/nav\"/>")
                        && !content.contains("root=\"/apps/cq/core/content/nav\" "),
                "Must not replace entire /apps/cq/core/content/nav (removes WKND Tools entries)");
    }

    @Test
    void structurePackage_mustNotDeclareBroadAppsRoot() throws Exception {
        final Path structurePom = REPO_ROOT.resolve("ui.apps.structure/pom.xml");
        assertTrue(Files.exists(structurePom), "Missing ui.apps.structure/pom.xml");

        final String content = Files.readString(structurePom);
        assertTrue(
                !content.contains("<root>/apps</root>"),
                "Structure package must not declare bare /apps (install would delete WKND)");
        assertTrue(
                !content.contains("<root>/apps/cq</root>"),
                "Structure package must not declare /apps/cq (install would delete cq overlays)");
        assertTrue(
                !content.contains("<root>/apps/wcm</root>"),
                "Structure package must not declare unrelated /apps/wcm root");
    }

    @Test
    void embeddedUiAppsInContainer_usesSafeNavMergeFilter() throws Exception {
        final Path allZip = REPO_ROOT.resolve(
                "all/target/searchstax-aem-connector.all-1.0.0-SNAPSHOT.zip");
        Assumptions.assumeTrue(
                Files.exists(allZip),
                "Skip until all module is packaged — run cross-platform-verify after mvn install");

        try (ZipFile all = new ZipFile(allZip.toFile())) {
            final ZipEntry uiAppsEntry =
                    all.stream()
                            .filter(e -> e.getName().contains("searchstax-aem-connector.ui.apps")
                                    && e.getName().endsWith(".zip"))
                            .findFirst()
                            .orElse(null);
            assertTrue(uiAppsEntry != null, "Container must embed ui.apps zip for Cloud CM deploys");

            final Path tempUiApps = Files.createTempFile("searchstax-ui-apps-", ".zip");
            try {
                Files.copy(all.getInputStream(uiAppsEntry), tempUiApps, StandardCopyOption.REPLACE_EXISTING);
                try (ZipFile uiApps = new ZipFile(tempUiApps.toFile())) {
                    final ZipEntry filterEntry = uiApps.getEntry("META-INF/vault/filter.xml");
                    assertTrue(filterEntry != null, "Embedded ui.apps must ship filter.xml");
                    final String filter = new String(uiApps.getInputStream(filterEntry).readAllBytes());
                    assertTrue(
                            filter.contains("/apps/cq/core/content/nav/tools/Searchstax"),
                            "Cloud-deployed ui.apps must merge Searchstax nav only");
                    assertTrue(filter.contains("mode=\"merge\""), "Cloud-deployed ui.apps must use merge filters");
                    assertTrue(
                            !filter.contains("root=\"/apps/cq/core/content/nav\"/>")
                                    && !filter.contains("root=\"/apps/cq/core/content/nav\" "),
                            "Cloud CM pipeline must not ship replace filter on /apps/cq/core/content/nav");
                }
            } finally {
                Files.deleteIfExists(tempUiApps);
            }
        }
    }

    @Test
    void containerPackage_doesNotEmbedStructurePackage() throws Exception {
        final Path allZip = REPO_ROOT.resolve(
                "all/target/searchstax-aem-connector.all-1.0.0-SNAPSHOT.zip");
        Assumptions.assumeTrue(
                Files.exists(allZip),
                "Skip until all module is packaged — run cross-platform-verify after mvn install");

        try (ZipFile zip = new ZipFile(allZip.toFile())) {
            final boolean hasStructure = zip.stream()
                    .anyMatch(e -> e.getName().contains("ui.apps.structure") && e.getName().endsWith(".zip"));
            assertTrue(!hasStructure, "Container must not embed ui.apps.structure (unsafe on Cloud install)");
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
