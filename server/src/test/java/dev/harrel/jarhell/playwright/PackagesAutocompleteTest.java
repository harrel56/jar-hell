package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import dev.harrel.jarhell.model.Gav;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@PlaywrightTest
class PackagesAutocompleteTest {
    private Page.WaitForSelectorOptions shortWaitOptions;
    private Locator ac;
    private final Driver driver;

    PackagesAutocompleteTest(Driver driver) {
        this.driver = driver;
    }

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
        insertGavs(IntStream.range(0, 60)
                .mapToObj(i -> new Gav("org.test", "artifact" + i, "1.0.0"))
                .toList()
        );
        ac.fill("org.test");
        // todo: this changes too fast- idk how to test this
//        page.waitForSelector(".lucide-loader-circle", shortWaitOptions).isVisible();
//        assertThat(page.locator(".lucide-search")).not().isAttached();
//        page.waitForSelector(".lucide-search", shortWaitOptions).isVisible();
//        assertThat(page.locator(".lucide-loader-circle")).not().isAttached();

        page.getByRole(AriaRole.OPTION).nth(0).waitFor();
        List<Locator> options = page.getByRole(AriaRole.OPTION).all();
        assertThat(options).hasSize(40);
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
        insertGavs(List.of(
                new Gav("org.test", "cycle1", "1.0.0"),
                new Gav("org.test", "cycle2", "1.0.0"),
                new Gav("org.test", "artifact", "1.0.0"),
                new Gav("org.test", "cycle3", "1.0.0")
        ));
        ac.fill("org.test");
        page.getByRole(AriaRole.OPTION).nth(2).click();
        assertThat(page).hasURL("/packages/org.test:artifact:3.2.1");
    }

    @Test
    void navigatesOnKeyboardEvents(Page page) {
        insertGavs(List.of(
                new Gav("org.test", "cycle1", "1.0.0"),
                new Gav("org.test", "cycle2", "1.0.0"),
                new Gav("org.test", "0", "1.0.0"),
                new Gav("org.test", "1", "1.0.0"),
                new Gav("org.test", "2", "1.0.0"),
                new Gav("org.test", "3", "1.0.0"),
                new Gav("org.test", "4", "1.0.0"),
                new Gav("org.test", "5", "1.0.0"),
                new Gav("org.test", "6", "1.0.0"),
                new Gav("org.test", "7", "1.0.0"),
                new Gav("org.test", "8", "1.0.0"),
                new Gav("org.test", "9", "1.0.0"),
                new Gav("org.test", "10", "1.0.0"),
                new Gav("org.test", "11", "1.0.0"),
                new Gav("org.test", "12", "1.0.0"),
                new Gav("org.test", "13", "1.0.0"),
                new Gav("org.test", "14", "1.0.0"),
                new Gav("org.test", "15", "1.0.0"),
                new Gav("org.test", "artifact", "1.0.0"),
                new Gav("org.test", "16", "1.0.0")
        ));
        ac.fill("org.test");
        page.getByRole(AriaRole.OPTION).nth(19).waitFor();
        page.keyboard().press("ArrowUp");
        page.keyboard().press("ArrowUp");
        page.keyboard().press("Enter");assertThat(page).hasURL("/packages/org.test:artifact:3.2.1");
    }

    @Test
    void longPackageNamesDoesntIncreaseOptionWidth(Page page) {
        insertGavs(List.of(
                new Gav("org.test", "cycle1", "1.0.0"),
                new Gav("org.test", "cycle2", "1.0.0"),
                new Gav("org.test", "artifact", "1.0.0"),
                new Gav("org.test", "long-artifact-name-long-artifact-name-long-artifact-name-long-artifact-name-long-artifact-name-long-artifact-name-long-artifact-name-long-artifact-name-long-artifact-name-long-artifact-name", "1.0.0")
        ));
        ac.fill("org.test");
        double acWidth = ac.boundingBox().width;
        double optionWidth = page.getByRole(AriaRole.OPTION).nth(3).boundingBox().width;
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

    void insertGavs(List<Gav> gavs) {
        String statement = gavs.stream()
                .map(gav -> "(:Artifact {groupId: '%s', artifactId: '%s', version: '%s'})"
                        .formatted(gav.groupId(), gav.artifactId(), gav.version()))
                .collect(Collectors.joining(",", "CREATE", ""));
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(statement));
        }
    }
}
