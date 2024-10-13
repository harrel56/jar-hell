package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class SmokeTest {
    @Test
    void loadsPackagesAutocomplete(Page page) {
        page.navigate("/");
        Locator acLocator = page.locator("#packages-autocomplete");
        assertThat(acLocator).isInViewport();
        assertThat(acLocator).isEditable();
    }

    @Test
    void clickingLogoRedirectsToHome(Page page) {
        page.navigate("/packages/test:test");
        page.getByAltText("hell in a jar").click();
        assertThat(page).hasURL("/");
    }

    @Test
    void clickingLogoTextRedirectsToHome(Page page) {
        page.navigate("/packages/test:test");
        page.getByText("Jarhell").click();
        assertThat(page).hasURL("/");
    }
}
