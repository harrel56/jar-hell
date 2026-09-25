package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class DependencyExplorerTest {
    private final Driver driver;

    DependencyExplorerTest(Driver driver) {
        this.driver = driver;
    }

    private Locator explorer(Page page) {
        return page.locator("section").filter(new Locator.FilterOptions().setHasText("Dependency explorer"));
    }

    /**
     * The coordinate link sits three levels below the row that holds the toggle and the metric columns.
     * Takes the first match, as a cycle makes the same coordinate appear more than once.
     */
    private Locator node(Page page, String coordinate) {
        return page.locator("a[title='%s']".formatted(coordinate)).first().locator("xpath=../../../..");
    }

    private Locator toggle(Locator node) {
        return node.getByRole(AriaRole.BUTTON);
    }

    @Test
    void showsOptionalAndUnresolvedDependencies(Page page) {
        Fixtures.insertIndexedVersions(driver, "dev.harrel", "json-schema", List.of("1.5.0"));
        page.navigate("/packages/dev.harrel:json-schema:1.5.0");
        assertThat(page.getByText("Effective size")).isInViewport();

        Locator explorer = explorer(page);
        assertThat(explorer.getByRole(AriaRole.ALERT))
                .containsText("The tree does not account for excluded packages or version conflicts");

        /* all seven dependencies are optional, and only jmail exists in the fixture repo.
           matched exactly, as a loose match is case-insensitive and would also hit the footer hint */
        Locator.GetByTextOptions exact = new Locator.GetByTextOptions().setExact(true);
        assertThat(explorer.getByText("Optional", exact)).hasCount(7);
        assertThat(explorer.getByText("Failed", exact)).hasCount(6);

        Locator jmail = node(page, "com.sanctionco.jmail:jmail:1.6.2");
        assertThat(jmail).containsText("30.63 KB");
        assertThat(jmail).containsText("MIT");

        /* unresolved nodes drop the size, java and license columns entirely */
        Locator unresolved = node(page, "org.json:json:20231013");
        assertThat(unresolved).containsText("Failed");
        assertThat(unresolved).not().containsText("KB");
    }

    @Test
    void expandsNodesLazilyAndTerminatesOnCycles(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "pre-cycle", List.of("1.0.0"));
        page.navigate("/packages/org.test:pre-cycle:1.0.0");
        assertThat(page.getByText("Effective size")).isInViewport();

        /* the root is expanded on load, so its single optional dependency is already listed */
        assertThat(toggle(node(page, "org.test:pre-cycle:1.0.0"))).hasAttribute("aria-expanded", "true");
        Locator cycle3 = node(page, "org.test:cycle3:1.0.0");
        assertThat(cycle3).containsText("Optional");
        assertThat(toggle(cycle3)).hasAttribute("aria-expanded", "false");
        assertThat(page.locator("a[title='org.test:cycle1:1.0.0']")).not().isAttached();

        /* cycle3 -> cycle1 -> cycle2 -> cycle3: each level is fetched only once opened */
        toggle(cycle3).click();
        assertThat(page.locator("a[title='org.test:cycle1:1.0.0']")).isVisible();

        toggle(node(page, "org.test:cycle1:1.0.0")).click();
        assertThat(page.locator("a[title='org.test:cycle2:1.0.0']")).isVisible();

        /* walking back into cycle3 renders a second row rather than recursing forever */
        toggle(node(page, "org.test:cycle2:1.0.0")).click();
        assertThat(page.locator("a[title='org.test:cycle3:1.0.0']")).hasCount(2);

        toggle(cycle3).click();
        assertThat(toggle(cycle3)).hasAttribute("aria-expanded", "false");
        assertThat(page.locator("a[title='org.test:cycle1:1.0.0']")).not().isAttached();
    }
}
