package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.harrel.jarhell.extension.EnvironmentExtension;
import dev.harrel.jarhell.extension.EnvironmentTest;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.*;
import java.util.Optional;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("playwright")
@EnvironmentTest
@UsePlaywright(PlaywrightTest.PlaywrightOptionsFactory.class)
public @interface PlaywrightTest {
    class PlaywrightOptionsFactory implements OptionsFactory {
        @Override
        public Options getOptions() {
            Options options = new Options();
            options.setBaseUrl("http://localhost:" + EnvironmentExtension.PORT);
            options.setHeadless(true);
            String browser = Optional.ofNullable(System.getenv("PLAYWRIGHT_BROWSER")).orElse("chromium");
            options.setBrowserName(browser);
            return options;
        }
    }
}

