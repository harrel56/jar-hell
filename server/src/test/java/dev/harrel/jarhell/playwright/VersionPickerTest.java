package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class VersionPickerTest {
    /** mirrors maven-metadata.xml of the reposilite fixture - the sidebar lists only versions known to the database */
    private static final List<String> VERSIONS = List.of(
            "1.0.0", "1.0.1", "1.0.2", "1.0.3", "1.0.4", "1.0.5", "1.0.6", "1.0.7", "1.0.8", "1.0.9", "1.0.10",
            "1.1.0", "1.1.1", "1.2.0", "1.3.0", "1.4.0", "1.4.1",
            "2.0.0", "2.0.1", "2.0.2", "2.0.3", "2.0.4", "2.0.5", "2.0.6", "2.0.7", "2.0.8", "2.1.0", "2.1.1",
            "2.2.0", "2.2.1", "2.3.0",
            "3.0.0", "3.0.1", "3.1.0", "3.2.0", "3.2.1"
    );

    private final Locator.GetByTextOptions options = new Locator.GetByTextOptions().setExact(true);
    private final Driver driver;
    /** the mobile VersionsPicker dialog renders the same labels, so every lookup is scoped to the rail */
    private Locator sidebar;

    VersionPickerTest(Driver driver) {
        this.driver = driver;
    }

    @BeforeEach
    void setUp(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", VERSIONS);
        sidebar = page.locator("aside");
    }

    /**
     * Matched on the href rather than the text, because an analysed version carries a pill inside the same
     * link - which makes the link's text "3.0.1Analyzed" and defeats an exact text match.
     */
    private Locator version(String version) {
        return sidebar.locator("a[href='/packages/org.test:artifact:%s']".formatted(version));
    }

    /**
     * Analysing a version revalidates the list behind the rail, which remounts it and drops whichever group
     * the test had opened. Rendered metrics do not tell us that has happened yet - the pill only appears once
     * the refreshed list has landed.
     */
    private void waitForRailToSettle(Page page) {
        assertThat(page.getByText("Effective size")).isInViewport();
        assertThat(sidebar.locator("[aria-current='true']").getByText("Analyzed")).isVisible();
    }

    @Test
    void groupsVersionsProperly(Page page) {
        page.navigate("/packages/org.test:artifact");
        assertThat(sidebar.getByText("1.0.x", options)).isVisible();
        assertThat(sidebar.getByText("1.1.x", options)).isVisible();
        assertThat(sidebar.getByText("1.2.x", options)).isVisible();
        assertThat(sidebar.getByText("1.3.x", options)).isVisible();
        assertThat(sidebar.getByText("1.4.x", options)).isVisible();
        assertThat(sidebar.getByText("2.x", options)).isVisible();
        assertThat(sidebar.getByText("3.x", options)).isVisible();

        assertThat(sidebar.getByText("1.x", options)).not().isVisible();
        assertThat(sidebar.getByText("2.0.x", options)).not().isVisible();
        assertThat(sidebar.getByText("2.1.x", options)).not().isVisible();
        assertThat(sidebar.getByText("2.2.x", options)).not().isVisible();
        assertThat(sidebar.getByText("3.0.x", options)).not().isVisible();
        assertThat(sidebar.getByText("3.1.x", options)).not().isVisible();
        assertThat(sidebar.getByText("3.2.x", options)).not().isVisible();

        assertThat(page).hasURL("/packages/org.test:artifact:3.2.1");
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(false))).hasCount(6);
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).hasCount(1);
    }

    @Test
    void expandsSectionDependingOnChosenVersion(Page page) {
        page.navigate("/packages/org.test:artifact:1.1.1");
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("1.1.x");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("1.1.1");

        page.navigate("/packages/org.test:artifact:2.0.8");
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("2.x");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("2.0.8");

        page.goBack();
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("1.1.x");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("1.1.1");

        page.goForward();
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("2.x");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("2.0.8");
    }

    @Test
    void navigatesToVersionAndAnalyzes(Page page) {
        // start off in another group, so that 1.0.x is collapsed and has to be expanded first. 1.0.10 is the
        // only version with a jar that no other test in this class analyses, so it is reliably unanalysed here
        page.navigate("/packages/org.test:artifact:1.1.0");
        waitForRailToSettle(page);

        sidebar.getByText("1.0.x", options).click();
        version("1.0.10").click();

        // asserted first, as the analysis of this tiny fixture jar finishes within a second or two
        assertThat(page.getByText("This version has not been analysed before")).isInViewport();
        assertThat(page).hasURL("/packages/org.test:artifact:1.0.10");
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("1.0.x");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("1.0.10");

        assertThat(page.getByText("Effective size")).isInViewport();
        assertThat(page.getByText("package 2.10 KB")).isInViewport();
        assertThat(page.getByText("transitive 30.63 KB")).isInViewport();
        assertThat(sidebar.locator("[aria-current='true']").getByText("Analyzed")).isVisible();
    }

    @Test
    void switchesVersionWhileAnalysisIsPending(Page page) {
        page.navigate("/packages/org.test:artifact:3.2.1");
        assertThat(page.getByText("This version has not been analysed before")).isInViewport();

        version("3.0.1").click();

        assertThat(page).hasURL("/packages/org.test:artifact:3.0.1");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("3.0.1");
        // the analysis of 3.2.1 is still in flight and must not replace the version we navigated to
        assertThat(page.getByText("implementation 'org.test:artifact:3.0.1'")).isVisible();
        assertThat(page.getByText("package 2.10 KB")).isInViewport();
        assertThat(page.getByText("implementation 'org.test:artifact:3.2.1'")).not().isAttached();
    }

    @Test
    void marksVersionAnalyzedAfterNavigatingAway(Page page) {
        page.navigate("/packages/org.test:artifact:3.2.1");
        assertThat(page.getByText("This version has not been analysed before")).isInViewport();

        version("3.0.1").click();
        assertThat(page).hasURL("/packages/org.test:artifact:3.0.1");

        // 3.2.1 finishes analysing while we sit on another version - the rail still has to pick that up
        assertThat(version("3.2.1").getByText("Analyzed")).isVisible();
    }

    @Test
    void expandsGroupAfterClientSideVersionChange(Page page) {
        page.navigate("/packages/org.test:artifact:1.1.0");
        waitForRailToSettle(page);
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("1.1.x");

        // opening an unrelated group overrides the rail's own idea of which one should be expanded
        sidebar.getByText("2.x", options).click();
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("2.x");

        // navigate to a third group client-side, via the header search, so the rail itself is never clicked
        Locator autocomplete = page.getByRole(AriaRole.COMBOBOX);
        autocomplete.fill("org.test:artifact:3.0.1");
        // dismiss the suggestions, otherwise Enter picks the first one instead of the typed coordinate
        page.keyboard().press("Escape");
        page.keyboard().press("Enter");

        assertThat(page).hasURL("/packages/org.test:artifact:3.0.1");
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("3.x");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("3.0.1");
    }

}
