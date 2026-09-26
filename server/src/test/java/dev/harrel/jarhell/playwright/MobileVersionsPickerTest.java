package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@MobilePlaywrightTest
class MobileVersionsPickerTest {
    private final Driver driver;

    MobileVersionsPickerTest(Driver driver) {
        this.driver = driver;
    }

    private Locator trigger(Page page, String version) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(version).setExact(true));
    }

    @Test
    void opensPicksVersionAndCloses(Page page) {
        // two majors and no minor with ten patches, so the groups come out as 1.x and 3.x
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("1.0.0", "1.1.0", "3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");
        assertThat(page.getByText("Effective size")).isInViewport();
        // below md the rail is replaced by this dialog
        assertThat(page.locator("aside")).not().isVisible();

        Locator dialog = page.locator("#versions-dialog");
        assertThat(dialog).not().isVisible();

        trigger(page, "3.0.1").click();

        assertThat(dialog).isVisible();
        Locator.GetByTextOptions exact = new Locator.GetByTextOptions().setExact(true);
        assertThat(dialog.getByText("1.x", exact)).isVisible();
        assertThat(dialog.getByText("3.x", exact)).isVisible();
        assertThat(dialog.locator("a[href='/packages/org.test:artifact:3.0.1']")).containsText("Analyzed");

        dialog.locator("a[href='/packages/org.test:artifact:1.1.0']").click();

        assertThat(page).hasURL("/packages/org.test:artifact:1.1.0");
        // navigating keeps the dialog mounted, so it has to have closed itself
        assertThat(dialog).not().isVisible();
        assertThat(trigger(page, "1.1.0")).isVisible();
    }

    @Test
    void closesWithoutNavigating(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.0", "3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");
        assertThat(page.getByText("Effective size")).isInViewport();

        Locator dialog = page.locator("#versions-dialog");
        trigger(page, "3.0.1").click();
        assertThat(dialog).isVisible();

        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close")).click();

        assertThat(dialog).not().isVisible();
        assertThat(page).hasURL("/packages/org.test:artifact:3.0.1");
    }
}
