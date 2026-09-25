package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@PlaywrightTest
class BadgesDialogTest {
    private final Driver driver;

    BadgesDialogTest(Driver driver) {
        this.driver = driver;
    }

    @Test
    void opensRendersBadgeAndCloses(Page page) {
        /* the badge endpoint redirects to shields.io - never let the test reach out to it */
        page.route("**://shields.io/**", route -> route.abort());
        Fixtures.insertIndexedVersions(driver, "org.test", "artifact", List.of("3.0.1"));
        page.navigate("/packages/org.test:artifact:3.0.1");
        assertThat(page.getByText("Effective size")).isInViewport();

        Locator dialog = page.locator("#badges-dialog");
        Locator badge = dialog.locator("img");
        /* a closed dialog is only display:none, so the body stays unrendered to keep the endpoint untouched */
        assertThat(dialog).not().isVisible();
        assertThat(badge).not().isAttached();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Badges")).click();

        assertThat(dialog).isVisible();
        assertThat(badge).hasAttribute("alt", "total size badge");
        assertThat(badge).hasAttribute("src", "http://localhost:8686/api/v1/badges/total_size/org.test:artifact:3.0.1");
        assertThat(dialog.locator("pre")).containsText(
                "[![total size](http://localhost:8686/api/v1/badges/total_size/org.test:artifact:3.0.1)]");

        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("package size")).click();
        assertThat(badge).hasAttribute("alt", "package size badge");
        assertThat(badge).hasAttribute("src", "http://localhost:8686/api/v1/badges/size/org.test:artifact:3.0.1");

        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close")).click();
        assertThat(dialog).not().isVisible();
    }
}
