package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class ErrorViewTest {
    @Test
    void unknownRoute(Page page) {
        page.navigate("/what");
        assertThat(page.getByRole(AriaRole.COMBOBOX)).isVisible();
        assertThat(page.getByText("404")).isVisible();
        assertThat(page.getByText("Page not found")).isVisible();
    }

    @Test
    void noPackage(Page page) {
        page.navigate("/packages/");
        assertThat(page.getByRole(AriaRole.COMBOBOX)).isVisible();
        assertThat(page.getByText("404")).isVisible();
        assertThat(page.getByText("Page not found")).isVisible();
    }

    /* a lone groupId is not a valid coordinate, so no `/packages/:coordinate` route matches it */
    @Test
    void onlyGroup(Page page) {
        page.navigate("/packages/org.test");
        assertThat(page.getByRole(AriaRole.COMBOBOX)).isVisible();
        assertThat(page.getByText("404")).isVisible();
        assertThat(page.getByText("Page not found")).isVisible();
    }

    @Test
    void invalidVersion(Page page) {
        page.navigate("/packages/org.test:artifact:0.0.1");
        assertThat(page.getByRole(AriaRole.COMBOBOX)).isVisible();
        assertThat(page.getByText("404")).isVisible();
        assertThat(page.getByText("Package not found")).isVisible();
        assertThat(page.getByText("org.test:artifact:0.0.1")).isVisible();
    }
}
