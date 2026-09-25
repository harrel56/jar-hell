package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        insertVersions();
        sidebar = page.locator("aside");
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
        // start off in another group, so that 3.x is collapsed and has to be expanded first
        page.navigate("/packages/org.test:artifact:1.1.0");
        sidebar.getByText("3.x", options).click();
        sidebar.getByText("3.0.1", options).click();

        // asserted first, as the analysis of this tiny fixture jar finishes within a second or two
        assertThat(page.getByText("This version has not been analysed before")).isInViewport();
        assertThat(page).hasURL("/packages/org.test:artifact:3.0.1");
        assertThat(sidebar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setExpanded(true))).containsText("3.x");
        assertThat(sidebar.locator("[aria-current='true']")).containsText("3.0.1");

        assertThat(page.getByText("Effective size")).isInViewport();
        assertThat(page.getByText("package 2.10 KB")).isInViewport();
        assertThat(page.getByText("transitive 30.63 KB")).isInViewport();
        assertThat(sidebar.locator("[aria-current='true']").getByText("Analyzed")).isVisible();
    }

    /**
     * Merges rather than creates, because an analysis kicked off by the previous test may still be running
     * and persist one of these versions between the database wipe and this call.
     */
    void insertVersions() {
        String statement = IntStream.range(0, VERSIONS.size())
                // `ON CREATE SET` writes exactly what the maven index importer stores for a not yet analysed version
                .mapToObj(i -> """
                        MERGE (v%d:Artifact {groupId: 'org.test', artifactId: 'artifact', version: '%s', classifier: ''})
                        ON CREATE SET v%d.fromMavenIndex = true, v%d.unresolved = true, v%d.unresolvedReason = 'initial-indexing'"""
                        .formatted(i, VERSIONS.get(i), i, i, i))
                .collect(Collectors.joining("\n"));
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(statement));
        }
    }
}
