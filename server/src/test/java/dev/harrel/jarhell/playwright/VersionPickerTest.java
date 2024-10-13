package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;


@PlaywrightTest
class VersionPickerTest {
    private final Page.GetByTextOptions options = new Page.GetByTextOptions();

    public VersionPickerTest() {
        options.setExact(true);
    }

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
    }
}
