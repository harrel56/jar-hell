package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@PlaywrightTest
class ThemeTest {
    private Locator themeButton;

    @BeforeEach
    void setUp(Page page) {
        page.navigate("/");
        // the only toggle in the top bar, labelled with the theme it switches to
        themeButton = page.locator("header button[aria-label$='theme']");
    }

    @Test
    void followsSystemByDefault(Page page) {
        assertNoStoredTheme(page);
        // playwright reports no `prefers-color-scheme` preference, which tokens.css resolves to light
        assertResolvedTheme(page, "light");
    }

    @Test
    void switchesToDarkMode(Page page) {
        themeButton.click();
        assertStoredTheme(page, "dark");
        assertResolvedTheme(page, "dark");
    }

    @Test
    void switchesBackToLightMode(Page page) {
        themeButton.click();
        themeButton.click();
        assertStoredTheme(page, "light");
        assertResolvedTheme(page, "light");
    }

    @Test
    void keepsChoiceAfterReload(Page page) {
        themeButton.click();
        assertStoredTheme(page, "dark");

        page.reload();
        assertStoredTheme(page, "dark");
        assertResolvedTheme(page, "dark");
    }

    /** the theme the inline script in index.html pins before first paint */
    void assertStoredTheme(Page page, String theme) {
        assertThat(page.evaluate("localStorage.getItem('theme')")).isEqualTo(theme);
        assertThat(page.evaluate("document.documentElement.dataset.theme ?? null")).isEqualTo(theme);
    }

    void assertNoStoredTheme(Page page) {
        assertThat(page.evaluate("localStorage.getItem('theme')")).isNull();
        assertThat(page.evaluate("document.documentElement.dataset.theme ?? null")).isNull();
    }

    /** what the app currently renders as, regardless of where the choice came from */
    void assertResolvedTheme(Page page, String theme) {
        String other = "dark".equals(theme) ? "light" : "dark";
        assertThat(themeButton.getAttribute("aria-label")).isEqualTo("Switch to %s theme".formatted(other));
    }
}
