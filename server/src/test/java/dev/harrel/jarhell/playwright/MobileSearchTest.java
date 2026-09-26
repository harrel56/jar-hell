package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@MobilePlaywrightTest
class MobileSearchTest {
    private final Driver driver;
    private Locator dialog;

    MobileSearchTest(Driver driver) {
        this.driver = driver;
    }

    @BeforeEach
    void setUp(Page page) {
        dialog = page.locator("#search-dialog");
    }

    private Locator searchIcon(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search packages"));
    }

    private void close(Page page) {
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close")).click();
        assertThat(dialog).not().isVisible();
        assertThat(searchIcon(page)).isVisible();
    }

    @Test
    void opensFromHeroStandInOnHome(Page page) {
        page.navigate("/");
        // the real hero field is there but hidden, so the stand-in is the only way into the search
        assertThat(page.locator("main input")).not().isVisible();
        assertThat(dialog).not().isVisible();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("group:artifact")).click();

        assertThat(dialog).isVisible();
        assertThat(page.getByRole(AriaRole.COMBOBOX)).isFocused();
        assertThat(page.getByRole(AriaRole.COMBOBOX)).hasValue("");
        close(page);
    }

    @Test
    void opensFromHeaderIconOnPackagePage(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");
        assertThat(page.locator("header input")).not().isVisible();
        assertThat(dialog).not().isVisible();

        searchIcon(page).click();

        assertThat(dialog).isVisible();
        assertThat(page.getByRole(AriaRole.COMBOBOX)).isFocused();
        // the field carries the coordinate of the page it was opened from, like the desktop header one
        assertThat(page.getByRole(AriaRole.COMBOBOX)).hasValue("org.test:artifact:3.0.1");
        close(page);
    }

    @Test
    void navigatesToAnotherPackageAndCloses(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.1"));
        Fixtures.insertIndexedVersions(driver, "org.test", "cycle1", List.of("1.0.0"));
        page.navigate("/packages/org.test:artifact:3.0.1");

        searchIcon(page).click();
        page.getByRole(AriaRole.COMBOBOX).fill("cycle1");
        page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("cycle1")).click();

        assertThat(page).hasURL("/packages/org.test:cycle1:1.0.0");
        assertThat(dialog).not().isVisible();
        assertThat(page.getByText("Effective size")).isInViewport();
    }

    @Test
    void navigatesOnFreeSoloAndCloses(Page page) {
        page.navigate("/");

        searchIcon(page).click();
        page.getByRole(AriaRole.COMBOBOX).fill("no.such:thing:1.0.0");
        // nothing matches, so Enter falls through to the typed coordinate instead of a suggestion
        assertThat(page.getByText("Nothing analysed under that name yet")).isVisible();
        page.keyboard().press("Enter");

        assertThat(page).hasURL("/packages/no.such:thing:1.0.0");
        assertThat(dialog).not().isVisible();
    }
}
