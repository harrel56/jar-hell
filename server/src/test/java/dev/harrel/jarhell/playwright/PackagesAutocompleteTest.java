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
    /** dropdown is capped at `max-h-96` (384px), which fits this many options of the hero variant */
    private static final int VISIBLE_OPTIONS = 8;

    private Locator ac;
    private final Driver driver;

    PackagesAutocompleteTest(Driver driver) {
        this.driver = driver;
    }

    @BeforeEach
    void setUp(Page page) {
        page.navigate("/");
        ac = page.getByRole(AriaRole.COMBOBOX);
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
    void slashShortcutRedirectsFocus(Page page) {
        ac.blur();
        assertThat(ac).not().isFocused();
        page.keyboard().press("/");
        assertThat(ac).isFocused();
        assertThat(ac).hasValue("");
    }

    @Test
    void displaysCappedNumberOfPackagesInViewport(Page page) {
        insertGavs(IntStream.range(0, 60)
                .mapToObj(i -> new Gav("org.test", "artifact" + i, "1.0.0"))
                .toList()
        );
        ac.fill("org.test");

        page.getByRole(AriaRole.OPTION).nth(0).waitFor();
        List<Locator> options = page.getByRole(AriaRole.OPTION).all();
        // the search endpoint caps its result set at 40
        assertThat(options).hasSize(40);
        List<Locator> visibleOptions = options.subList(0, VISIBLE_OPTIONS);
        List<Locator> hiddenOptions = options.subList(VISIBLE_OPTIONS, options.size());

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
        assertThat(page.getByText("Nothing analysed under that name yet")).isVisible();
    }

    @Test
    void closesOptionsOnEscape(Page page) {
        ac.fill("ui-test2");
        assertThat(page.getByText("Nothing analysed under that name yet")).isVisible();
        page.keyboard().press("Escape");
        assertThat(page.getByText("Nothing analysed under that name yet")).not().isVisible();
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
        // picked by name rather than by index - neo4j does not promise any order for the search results
        page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("artifact")).click();
        // the option links to the versionless coordinate, which redirects to the newest known version
        assertThat(page).hasURL("/packages/org.test:artifact:1.0.0");
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
        // the search result order is up to neo4j, so the expected target is read off the rendered list
        Locator target = page.getByRole(AriaRole.OPTION).nth(18);
        String href = target.getAttribute("href");

        // no option is active yet, so the first ArrowUp wraps to the last one
        page.keyboard().press("ArrowUp");
        page.keyboard().press("ArrowUp");
        assertThat(target).hasAttribute("aria-selected", "true");

        page.keyboard().press("Enter");
        // the option links to the versionless coordinate, which redirects to the newest known version
        assertThat(page).hasURL(href + ":1.0.0");
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
        page.getByRole(AriaRole.OPTION).nth(3).waitFor();
        // the field is wrapped in a label, which spans the whole width of the autocomplete
        double fieldWidth = ac.locator("xpath=..").boundingBox().width;
        double shortOptionWidth = page.getByRole(AriaRole.OPTION).nth(2).boundingBox().width;
        double longOptionWidth = page.getByRole(AriaRole.OPTION).nth(3).boundingBox().width;
        assertThat(longOptionWidth).isEqualTo(shortOptionWidth);
        assertThat(longOptionWidth).isLessThanOrEqualTo(fieldWidth);
    }

    @Test
    void clickingEnterWhenEmptyDoesNothing(Page page) {
        assertThat(ac).hasValue("");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/");
    }

    @Test
    void blurDoesNotWorkForFreeSolo(Page page) {
        ac.fill("test");
        ac.blur();
        assertThat(page).hasURL("/");
    }

    @Test
    void cannotFreeSoloSingleToken(Page page) {
        ac.fill("test");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/");
    }

    @Test
    void canFreeSoloGroupWithArtifactId(Page page) {
        ac.fill("test-group:test-id");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/packages/test-group:test-id");
    }

    @Test
    void canFreeSoloGroupWithArtifactIdWithVersion(Page page) {
        ac.fill("test-group:test-id:1.0.0");
        page.keyboard().press("Enter");
        assertThat(page).hasURL("/packages/test-group:test-id:1.0.0");
    }

    void insertGavs(List<Gav> gavs) {
        String statement = gavs.stream()
                // only artifacts known to the maven index are searchable, and `findAllVersions` matches on a non-null classifier
                .map(gav -> "(:Artifact {groupId: '%s', artifactId: '%s', version: '%s', classifier: '', fromMavenIndex: true})"
                        .formatted(gav.groupId(), gav.artifactId(), gav.version()))
                .collect(Collectors.joining(",", "CREATE", ""));
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(statement));
        }
    }
}
