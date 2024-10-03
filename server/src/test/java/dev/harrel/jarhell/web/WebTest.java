package dev.harrel.jarhell.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import dev.harrel.jarhell.extension.PlaywrightTest;
import org.junit.jupiter.api.Test;

@PlaywrightTest
class WebTest {
    @Test
    void shouldClickButton(Page page, Browser browser) {
        System.out.println(browser.browserType().name());
        page.navigate("/");
//        page.locator("button").click();
//        page.waitForCondition(() -> false);
//        assertEquals("Clicked", page.evaluate("result"));
    }
}
