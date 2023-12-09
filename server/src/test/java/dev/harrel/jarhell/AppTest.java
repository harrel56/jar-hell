package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class AppTest extends Neo4jTest {

    @Test
    void name() {
        ArtifactRepository artifactRepository = new ArtifactRepository(driver, new ObjectMapper());
        Optional<ArtifactTree> artifact = artifactRepository.find(new Gav("dev.harrel", "json-schema", "1.3.1"));
        System.out.println(artifact);
    }
}