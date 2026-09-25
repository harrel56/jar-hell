package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The UI hides the {@code sources} and {@code javadoc} classifiers, which are the only ones any fixture
 * publishes - so a meaningful variant has to be seeded directly.
 */
@PlaywrightTest
class ClassifierVariantsTest {
    private final Driver driver;

    ClassifierVariantsTest(Driver driver) {
        this.driver = driver;
    }

    @BeforeEach
    void setUp() {
        Fixtures.run(driver, """
                MERGE (main:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: ''})
                ON CREATE SET main.classifiers = ['jakarta', 'sources']
                MERGE (variant:Artifact {groupId: 'org.test', artifactId: 'lib', version: '1.0.0', classifier: 'jakarta'})
                ON CREATE SET variant.classifiers = ['jakarta', 'sources']""");
    }

    /**
     * Matched by accessible name rather than by the coordinate in `title`, which the dependency explorer
     * puts on its own links too.
     */
    private Locator variant(Page page, String name) {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    @Test
    void listsVariantsOnTheMainArtifact(Page page) {
        page.navigate("/packages/org.test:lib:1.0.0");

        assertThat(page.getByText("Variants")).isVisible();
        assertThat(variant(page, "main artifact no classifier"))
                .hasAttribute("href", "/packages/org.test:lib:1.0.0");
        assertThat(variant(page, "jakarta classifier"))
                .hasAttribute("href", "/packages/org.test:lib:1.0.0:jakarta");
        /* sources is published but deliberately hidden from the strip */
        assertThat(variant(page, "sources classifier")).not().isAttached();
        assertThat(page.getByText("You are viewing a classifier artifact")).not().isAttached();
    }

    @Test
    void warnsOnAClassifierArtifactAndNavigatesBack(Page page) {
        page.navigate("/packages/org.test:lib:1.0.0:jakarta");

        assertThat(page.getByText("You are viewing a classifier artifact")).isVisible();
        assertThat(page.getByText("Everything below describes the")).containsText("jakarta");

        variant(page, "main artifact no classifier").click();
        assertThat(page).hasURL("/packages/org.test:lib:1.0.0");
        assertThat(page.getByText("You are viewing a classifier artifact")).not().isAttached();
    }
}
