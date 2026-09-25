package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Renders real analyser output rather than hand-seeded nodes - only the version rows are seeded, the analysis
 * itself runs against the reposilite fixture.
 */
@PlaywrightTest
class PackageViewTest {
    private final Driver driver;

    PackageViewTest(Driver driver) {
        this.driver = driver;
    }

    /** value, unit and label share one flex row, so the label's grandparent is the whole metric */
    private Locator metric(Page page, String label) {
        return page.getByText(label, new Page.GetByTextOptions().setExact(true)).locator("xpath=../..");
    }

    @Test
    void rendersEffectiveCostAndJarSections(Page page) {
        Fixtures.insertIndexedVersions(driver, "dev.harrel", "json-schema", List.of("1.5.0"));
        page.navigate("/packages/dev.harrel:json-schema:1.5.0");

        /* json-schema pulls in nothing required, but declares seven optional dependencies */
        assertThat(metric(page, "Effective size")).containsText("174.34");
        assertThat(page.getByText("package 174.34 KB")).isVisible();
        assertThat(page.getByText("transitive 0 bytes")).isVisible();
        assertThat(metric(page, "Required dependencies")).containsText("0");
        assertThat(metric(page, "Required dependencies")).containsText("Zero-dep");
        assertThat(metric(page, "Optional dependencies")).containsText("7");
        assertThat(page.getByText("Runs on every current LTS. Class file 52.")).isVisible();
        assertThat(page.getByText("1 distinct license.")).isVisible();

        /* inside this jar */
        assertThat(page.getByText("json-schema-1.5.0.jar only - dependencies excluded")).isVisible();
        assertThat(page.getByText("128 entries")).isVisible();
        assertThat(page.getByText("42% of 113 types are reachable from outside the jar.")).isVisible();
        assertThat(page.getByText("no module name declared")).isVisible();
        assertThat(page.getByText("Gradle module metadata")).isVisible();
        /* the jar ships no META-INF/services, which leaves the accordion empty and unclickable */
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Services provided"))).isDisabled();
    }

    @Test
    void rendersMultipleEffectiveLicensesAndPublishedArtifacts(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");

        assertThat(metric(page, "Required dependencies")).containsText("1");
        assertThat(metric(page, "Required dependencies")).containsText("Few");
        assertThat(page.getByText("Requires Java 21 or newer. Class file 65.")).isVisible();
        /* the artifact itself declares no license, jmail contributes MIT - so the rollup spans two */
        assertThat(metric(page, "Effective license")).containsText("No license");
        assertThat(page.getByText("2 distinct licenses.")).isVisible();

        /* sources and javadoc are published alongside this version, unlike json-schema */
        assertThat(page.getByText("artifact-3.0.1-sources.jar")).isVisible();
        assertThat(page.getByText("artifact-3.0.1-javadoc.jar")).isVisible();
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Browse")))
                .hasAttribute("href", "https://javadoc.io/doc/org.test/artifact/3.0.1");
    }
}
