package dev.harrel.jarhell;

import io.avaje.config.Config;
import org.junit.jupiter.api.Test;

@EnvironmentTest
class AppTest {

    @Test
    void name() {
        String s = Config.get("maven.local-repo.path");
        System.out.println(s);
    }
}

@EnvironmentTest
class AppTest2 {

    @Test
    void name() {
        String s = Config.get("maven.local-repo.path");
        System.out.println(s);
    }
}