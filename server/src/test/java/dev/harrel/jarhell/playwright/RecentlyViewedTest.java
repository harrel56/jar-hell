package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Only the cross-page integration is covered here - PackagePage writes the entry, HomePage reads it.
 * Dedup, version replacement and the cap of 6 are unit-tested in web/src/utils/recentlyViewed.test.ts.
 */
@PlaywrightTest
class RecentlyViewedTest {
    private final Driver driver;

    RecentlyViewedTest(Driver driver) {
        this.driver = driver;
    }

    @Test
    void remembersVisitedPackageOnHomePage(Page page) {
        page.navigate("/");
        assertThat(page.getByText("Recently viewed")).not().isAttached();

        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");
        /* the entry is only remembered once the analysis lands */
        assertThat(page.getByText("Effective size")).isInViewport();

        page.getByText("Jarhell").click();
        assertThat(page).hasURL("/");

        /* scoped to the section, as "recently analysed" lists the same package right after we analysed it */
        Locator recentlyViewed = page.locator("section")
                .filter(new Locator.FilterOptions().setHasText("Recently viewed"));
        assertThat(recentlyViewed.getByText("on this machine")).isVisible();
        assertThat(recentlyViewed.locator("a[href='/packages/org.test:artifact:3.0.1']")).isVisible();
    }
}
