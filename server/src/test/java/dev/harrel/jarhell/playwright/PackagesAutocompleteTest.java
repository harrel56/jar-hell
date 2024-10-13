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

    @Test
    void displaysNotFound(Page page) {
        ac.fill("ui-test2");
        assertThat(page.getByText("No results found")).isVisible();
    }

    @Test
    void closesOptionsOnEscape(Page page) {
        ac.fill("ui-test2");
        assertThat(page.getByText("No results found")).isVisible();
        page.keyboard().press("Escape");
        assertThat(page.getByText("No results found")).not().isVisible();
        assertThat(ac).isFocused();
    }

    @Test
    void navigatesOnOptionClick(Page page) {
        ac.fill("ui-test");
        page.getByRole(AriaRole.OPTION).nth(2).click();
        assertThat(page).hasURL("/packages/test-group2:ui-test2:1.0.0");
    }

    @Test
    void navigatesOnKeyboardEvents(Page page) {
        ac.fill("ui-test");
        page.getByRole(AriaRole.OPTION).nth(19).waitFor();
        page.keyboard().press("ArrowUp");
        page.keyboard().press("ArrowUp");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/packages/test-group19:ui-test19:1.0.0");
    }

    @Test
    void longPackageNamesDoesntIncreaseOptionWidth(Page page) {
        ac.fill("ui-test");
        double acWidth = ac.boundingBox().width;
        double optionWidth = page.getByRole(AriaRole.OPTION).nth(19).boundingBox().width;
        assertThat(acWidth).isGreaterThan(optionWidth);
    }

    @Test
    void clickingEnterWhenEmptyDoesNothing(Page page) {
        page.navigate("/packages/test:test");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/packages/test:test");
    }

    @Test
    void blurDoesNotWorkForFreeSolo(Page page) {
        ac.fill("test");
        ac.blur();
        assertThat(page).hasURL("/");
    }

    @Test
    void canFreeSoloSingleToken(Page page) {
        ac.fill("test");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/packages/test:test");
    }

    @Test
    void canFreeSoloGroupWithArtifactId(Page page) {
        ac.fill("test-group:test-id");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/packages/test-group:test-id");
    }

    @Test
    void cannotFreeSoloGroupWithArtifactIdWithVersion(Page page) {
        ac.fill("test-group:test-id:1.0.0");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/packages/test-group:test-id");
    }
}
