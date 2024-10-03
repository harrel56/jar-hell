package dev.harrel.jarhell.extension;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import com.microsoft.playwright.junit.UsePlaywright;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@EnvironmentTest
@UsePlaywright(PlaywrightTest.PlaywrightOptionsFactory.class)
public @interface PlaywrightTest {
    class PlaywrightOptionsFactory implements OptionsFactory {
        @Override
        public Options getOptions() {
            Options options = new Options();
            options.setBaseUrl("http://localhost:8060/");
            options.setHeadless(true);
            options.setBrowserName("chromium");
            return options;
        }
    }
}

