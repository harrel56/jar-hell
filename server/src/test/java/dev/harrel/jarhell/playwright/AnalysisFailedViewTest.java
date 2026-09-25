package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class AnalysisFailedViewTest {
    /** the server stops retrying an artifact after this many failures */
    private static final int MAX_ATTEMPTS = 10;

    private final Driver driver;

    AnalysisFailedViewTest(Driver driver) {
        this.driver = driver;
    }

    private Locator retryButton(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Try again"));
    }

    @Test
    void showsReasonAndAttempts(Page page) {
        /* 1.1.1 is listed in maven-metadata.xml but has no directory in the fixture repo */
        Fixtures.insertFailedVersion(driver, "org.test", "artifact", "1.1.1", "boom", 3);
        page.navigate("/packages/org.test:artifact:1.1.1");

        assertThat(page.getByText("Analysis failed")).isVisible();
        assertThat(page.getByText("Jarhell fetched this version but could not measure it. No metrics are available.")).isVisible();
        assertThat(page.getByText("Reason")).isVisible();
        assertThat(page.getByText("boom")).isVisible();
        assertThat(page.getByText("Attempts")).isVisible();
        assertThat(retryButton(page)).isEnabled();
        assertThat(page.getByText("no further automatic retries")).not().isAttached();
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Report a problem")))
                .hasAttribute("href", "https://github.com/harrel56/jar-hell/issues/new?title=Analysis%20failed%3A%20org.test%3Aartifact%3A1.1.1");

        /* the rail marks it too */
        assertThat(page.locator("aside").locator("[aria-current='true']").getByText("Failed")).isVisible();
    }

    @Test
    void givesUpAfterMaxAttempts(Page page) {
        Fixtures.insertFailedVersion(driver, "org.test", "artifact", "1.1.1", "boom", MAX_ATTEMPTS);
        page.navigate("/packages/org.test:artifact:1.1.1");

        assertThat(page.getByText("no further automatic retries")).isVisible();
        assertThat(retryButton(page)).isDisabled();
    }

    @Test
    void retryRecoversWhenTheArtifactIsAnalysable(Page page) {
        /* unlike 1.1.1, this version does have a jar in the fixture repo, so re-analysing it succeeds */
        Fixtures.insertFailedVersion(driver, "org.test", "artifact", "3.0.1", "boom", 3);
        page.navigate("/packages/org.test:artifact:3.0.1");
        assertThat(page.getByText("Analysis failed")).isVisible();

        retryButton(page).click();

        assertThat(page.getByText("Effective size")).isInViewport();
        assertThat(page.getByText("package 2.10 KB")).isInViewport();
        assertThat(page.getByText("Analysis failed")).not().isAttached();
    }
}
