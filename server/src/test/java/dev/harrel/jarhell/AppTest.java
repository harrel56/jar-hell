package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.config.Config;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class AppTest {

    @Test
    void name() {
        String s = Config.get("maven.local-repo.path");
        System.out.println(s);
    }
}