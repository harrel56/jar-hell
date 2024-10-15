package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class ErrorViewTest {
    @Test
    void unknownRoute(Page page) {
        page.navigate("/what");
        assertThat(page.locator("#packages-autocomplete")).isVisible();
        assertThat(page.getByText("404")).isVisible();
        assertThat(page.getByText("Resource not found")).isVisible();
    }

    @Test
    void noPackage(Page page) {
        page.navigate("/packages/");
        assertThat(page.locator("#packages-autocomplete")).isVisible();
        assertThat(page.getByText("404")).isVisible();
        assertThat(page.getByText("Resource not found")).isVisible();
    }

    @Test
    void onlyGroup(Page page) {
        page.navigate("/packages/org.test");
        assertThat(page.locator("#packages-autocomplete")).isVisible();
        assertThat(page.getByText("400")).isVisible();
        assertThat(page.getByText("Package format is invalid")).isVisible();
    }

    @Test
    void invalidVersion(Page page) {
        page.navigate("/packages/org.test:artifact:0.0.1");
        assertThat(page.locator("#packages-autocomplete")).isVisible();
        assertThat(page.getByText("404")).isVisible();
        assertThat(page.getByText("Version 0.0.1 not found")).isVisible();
    }
}
