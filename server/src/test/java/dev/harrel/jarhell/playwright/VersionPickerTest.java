package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;


@PlaywrightTest
class VersionPickerTest {
    private final Page.GetByTextOptions options = new Page.GetByTextOptions().setExact(true);

    @Test
    void groupsVersionsProperly(Page page) {
        page.navigate("/packages/org.test:artifact");
        assertThat(page.getByText("1.0.x", options)).isVisible();
        assertThat(page.getByText("1.1.x", options)).isVisible();
        assertThat(page.getByText("1.2.x", options)).isVisible();
        assertThat(page.getByText("1.3.x", options)).isVisible();
        assertThat(page.getByText("1.4.x", options)).isVisible();
        assertThat(page.getByText("2.x", options)).isVisible();
        assertThat(page.getByText("3.x", options)).isVisible();

        assertThat(page.getByText("1.x, options")).not().isVisible();
        assertThat(page.getByText("2.0.x", options)).not().isVisible();
        assertThat(page.getByText("2.1.x", options)).not().isVisible();
        assertThat(page.getByText("2.2.x", options)).not().isVisible();
        assertThat(page.getByText("3.0.x", options)).not().isVisible();
        assertThat(page.getByText("3.1.x", options)).not().isVisible();
        assertThat(page.getByText("3.2.x", options)).not().isVisible();

        assertThat(page).hasURL("/packages/org.test:artifact:3.2.1");
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setExpanded(false)).getByText("items")).hasCount(6);
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setExpanded(true)).getByText("items")).hasCount(1);
    }

    @Test
    void expandsSectionDependingOnChosenVersion(Page page) {
        page.navigate("/packages/org.test:artifact:1.1.1");
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setExpanded(true))).containsText("1.1.x");
        assertThat(page.locator("[aria-current='true']")).containsText("1.1.1");

        page.navigate("/packages/org.test:artifact:2.0.8");
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setExpanded(true))).containsText("2.x");
        assertThat(page.locator("[aria-current='true']")).containsText("2.0.8");

        page.goBack();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setExpanded(true))).containsText("1.1.x");
        assertThat(page.locator("[aria-current='true']")).containsText("1.1.1");

        page.goForward();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setExpanded(true))).containsText("2.x");
        assertThat(page.locator("[aria-current='true']")).containsText("2.0.8");
    }

    @Test
    void navigatesToVersionAndAnalyzes(Page page) {
        page.navigate("/packages/org.test:artifact");
        page.getByText("3.x", options).click();
        page.getByText("3.0.1", options).click();

        assertThat(page).hasURL("/packages/org.test:artifact:3.0.1");
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setExpanded(true))).containsText("3.x");
        assertThat(page.locator("[aria-current='true']")).containsText("3.0.1");
        assertThat(page.locator("[aria-current='true']").getByText("Analyzed")).not().isAttached();

        assertThat(page.getByText("Analysis is in progress")).isInViewport();
        assertThat(page.getByText("32.73KB").nth(0)).isInViewport();
        assertThat(page.locator("[aria-current='true']").getByText("Analyzed")).isVisible();
    }
}
