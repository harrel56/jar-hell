package dev.harrel.jarhell.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.harrel.jarhell.extension.EnvironmentTest;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("playwright")
@EnvironmentTest
@UsePlaywright(MobilePlaywrightTest.MobileOptionsFactory.class)
public @interface MobilePlaywrightTest {
    /**
     * Only the viewport is emulated, not the whole device - a device descriptor would also switch on
     * touch input, which firefox rejects and PLAYWRIGHT_BROWSER can still select.
     */
    class MobileOptionsFactory extends PlaywrightTest.PlaywrightOptionsFactory {
        @Override
        public Options getOptions() {
            return super.getOptions()
                    .setContextOptions(new Browser.NewContextOptions().setViewportSize(390, 844));
        }
    }
}
