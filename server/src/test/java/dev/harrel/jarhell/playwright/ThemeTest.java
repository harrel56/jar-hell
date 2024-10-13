package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.ElementHandle;
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
        themeButton = page.getByText("Toggle theme");
    }

    @Test
    void isSystemByDefault(Page page) {
        assertLightTheme(page);
        themeButton.click();
        page.getByText("System").click();
        assertLightTheme(page);
    }

    @Test
    void switchesToLightMode(Page page) {
        themeButton.click();
        page.getByText("Light").click();
        assertLightTheme(page);
    }

    @Test
    void switchesToDarkMode(Page page) {
        themeButton.click();
        page.getByText("Dark").click();
        assertDarkTheme(page);
    }

    void assertLightTheme(Page page) {
        ElementHandle rootHandle = page.querySelector(":root");
        assertThat(rootHandle.getAttribute("class")).isEqualTo("light");
    }

    void assertDarkTheme(Page page) {
        ElementHandle rootHandle = page.querySelector(":root");
        assertThat(rootHandle.getAttribute("class")).isEqualTo("dark");
    }
}
