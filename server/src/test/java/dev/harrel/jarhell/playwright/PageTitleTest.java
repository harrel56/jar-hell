package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class PageTitleTest {
    private final Driver driver;

    PageTitleTest(Driver driver) {
        this.driver = driver;
    }

    @Test
    void homeKeepsTheBareTitle(Page page) {
        page.navigate("/");
        assertThat(page).hasTitle("Jar Hell");
    }

    @Test
    void packagePageCarriesTheCoordinate(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");
        assertThat(page).hasTitle("Jar Hell | org.test:artifact:3.0.1");

        page.navigate("/packages/org.test:lib:1.0.0:jakarta");
        assertThat(page).hasTitle("Jar Hell | org.test:lib:1.0.0:jakarta");
    }

    @Test
    void titleFollowsClientSideNavigation(Page page) {
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");
        assertThat(page).hasTitle("Jar Hell | org.test:artifact:3.0.1");

        page.getByAltText("hell in a jar").click();
        assertThat(page).hasURL("/");
        // leaving the package page has to hand the title back to the app-wide default
        assertThat(page).hasTitle("Jar Hell");

        page.getByRole(AriaRole.COMBOBOX).fill("org.test:artifact:3.0.0");
        page.keyboard().press("Escape");
        page.keyboard().press("Enter");
        assertThat(page).hasTitle("Jar Hell | org.test:artifact:3.0.0");
    }
}
