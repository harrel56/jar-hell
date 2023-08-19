package dev.harrel.jarhell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.neo4j.driver.Values.parameters;

public class ArtifactRepository {
    private final Driver driver;
    private final ObjectMapper objectMapper;

    public ArtifactRepository(Driver driver, ObjectMapper objectMapper) {
        this.driver = driver;
        this.objectMapper = objectMapper;
    }

    public Optional<ArtifactInfo> find(Gav gav) {
        Map<String, Object> gavData = objectMapper.convertValue(gav, new TypeReference<>() {});
        try (var session = driver.session()) {
            List<ArtifactInfo> artifactInfos = session.executeRead(tx -> tx.run(new Query("""
                            MATCH (a:Artifact {groupId: $props.groupId, artifactId: $props.artifactId, version: $props.version})-[rel:DEPENDS_ON*..]->(d:Artifact)
                            RETURN a,rel,d""",
                            parameters("props", gavData))
                    ).list(r -> {
                        Map<String, Object> dataMap = r.get("a").asNode().asMap();
                        return objectMapper.convertValue(dataMap, ArtifactInfo.class);
                    })
            );
            if (artifactInfos.size() > 1) {
                throw new IllegalArgumentException("Too many results");
            } else if (artifactInfos.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(artifactInfos.get(0));
            }
        }
    }

    public void save(ArtifactTree artifactTree) {
        Map<String, Object> propsMap = objectMapper.convertValue(artifactTree.artifactInfo(), new TypeReference<>() {});
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(new Query("CREATE (a:Artifact $props)",
                            parameters("props", propsMap))
                    )
            );

            Map<String, Object> parentGav = toGavMap(artifactTree.artifactInfo());
            artifactTree.dependencies().forEach(dep -> {
                        Map<String, Object> depGav = toGavMap(dep.artifactInfo());
                        session.executeWriteWithoutResult(tx -> tx.run(new Query("""
                                MATCH (a:Artifact {groupId: $parentGav.groupId, artifactId: $parentGav.artifactId, version: $parentGav.version}),
                                      (d:Artifact {groupId: $depGav.groupId, artifactId: $depGav.artifactId, version: $depGav.version})
                                CREATE (a)-[:DEPENDS_ON]->(d)""",
                                parameters("parentGav", parentGav, "depGav", depGav))));
                    }
            );
        }
    }

    private Map<String, Object> toGavMap(ArtifactInfo info) {
        return Map.of(
                "groupId", info.groupId(),
                "artifactId", info.artifactId(),
                "version", info.version()
        );
    }
}
