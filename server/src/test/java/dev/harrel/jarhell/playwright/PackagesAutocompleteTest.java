package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@PlaywrightTest
class PackagesAutocompleteTest {
    private Page.WaitForSelectorOptions shortWaitOptions;
    private Locator ac;

    @BeforeEach
    void setUp(Page page) {
        page.navigate("/");
        ac = page.locator("#packages-autocomplete");
        shortWaitOptions = new Page.WaitForSelectorOptions();
        shortWaitOptions.setTimeout(2000);
    }

    @Test
    void hasAutofocus() {
        assertThat(ac).isFocused();
    }

    @Test
    void magnifyingGlassRedirectsFocus(Page page) {
        page.locator(".lucide-search").click();
        assertThat(ac).isFocused();
    }

    @Test
    void displays8PackagesInViewport(Page page) {
        ac.fill("ui-test");
        page.waitForSelector(".lucide-loader-circle", shortWaitOptions).isVisible();
        assertThat(page.locator(".lucide-search")).not().isAttached();
        page.waitForSelector(".lucide-search", shortWaitOptions).isVisible();
        assertThat(page.locator(".lucide-loader-circle")).not().isAttached();

        List<Locator> options = page.getByRole(AriaRole.OPTION).all();
        assertThat(options).hasSize(20);
        List<Locator> visibleOptions = options.subList(0, 8);
        List<Locator> hiddenOptions = options.subList(8, options.size());

        for (Locator visibleOption : visibleOptions) {
            assertThat(visibleOption).isInViewport();
        }
        for (Locator hiddenOption : hiddenOptions) {
            assertThat(hiddenOption).not().isInViewport();
        }
        assertThat(ac).isFocused();
    }
}
