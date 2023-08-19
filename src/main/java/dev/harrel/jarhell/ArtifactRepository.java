package dev.harrel.jarhell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.*;
import java.util.stream.StreamSupport;

import static org.neo4j.driver.Values.parameters;

public class ArtifactRepository {
    private final Driver driver;
    private final ObjectMapper objectMapper;

    public ArtifactRepository(Driver driver, ObjectMapper objectMapper) {
        this.driver = driver;
        this.objectMapper = objectMapper;
    }

    public Optional<ArtifactTree> find(Gav gav) {
        Map<String, Object> gavData = objectMapper.convertValue(gav, new TypeReference<>() {});
        try (var session = driver.session()) {
            Map<Gav, AggregateTree> result = new LinkedHashMap<>();
            /* todo: this seems awfully unoptimal - lots of duplicated data. Maybe something like this could be used:
                MATCH (n)
                OPTIONAL MATCH (n)-[r]-(m)
                RETURN COLLECT(DISTINCT n) AS nodes, COLLECT(DISTINCT r) AS relationships
             */
            List<Path> relations = session.executeRead(tx -> tx.run(new Query("""
                            MATCH x = (:Artifact {groupId: $props.groupId, artifactId: $props.artifactId, version: $props.version})-[DEPENDS_ON*0..]->(d:Artifact)
                            RETURN x
                            ORDER BY d.groupId, d.artifactId, d.version""",
                            parameters("props", gavData))
                    ).list(r -> r.get("x").asPath())
            );

            for (Path path : relations) {
                Map<Gav, AggregateTree> currentLevel = result;
                List<Node> nodes = StreamSupport.stream(path.nodes().spliterator(), false).toList();
                List<Relationship> relationships = StreamSupport.stream(path.relationships().spliterator(), false).toList();
                for (int i = 0; i < nodes.size(); i++) {
                    Node node = nodes.get(i);
                    RelationProps relationProps = i < 1 ? null : objectMapper.convertValue(relationships.get(i - 1).asMap(), RelationProps.class);
                    ArtifactInfo data = toArtifactInfo(node);
                    Gav dataGav = toGav(data);
                    currentLevel.computeIfAbsent(dataGav, k -> new AggregateTree(data, relationProps));
                    currentLevel = currentLevel.get(dataGav).deps;
                }
            }

            if (result.size() > 1) {
                throw new IllegalArgumentException("Too many results");
            }  else {
                return result.values().stream()
                        .map(AggregateTree::toArtifactTree)
                        .findFirst();
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
                Map<String, Boolean> depProps = Map.of("optional", dep.optional());
                Map<String, Object> depGav = toGavMap(dep.artifact().artifactInfo());
                        session.executeWriteWithoutResult(tx -> tx.run(new Query("""
                                MATCH (a:Artifact {groupId: $parentGav.groupId, artifactId: $parentGav.artifactId, version: $parentGav.version}),
                                      (d:Artifact {groupId: $depGav.groupId, artifactId: $depGav.artifactId, version: $depGav.version})
                                CREATE (a)-[:DEPENDS_ON $depProps]->(d)""",
                                parameters("parentGav", parentGav, "depGav", depGav, "depProps", depProps))));
                    }
            );
        }
    }

    private Gav toGav(ArtifactInfo info) {
        return new Gav(info.groupId(), info.artifactId(), info.version());
    }

    private Map<String, Object> toGavMap(ArtifactInfo info) {
        return Map.of(
                "groupId", info.groupId(),
                "artifactId", info.artifactId(),
                "version", info.version()
        );
    }

    private ArtifactInfo toArtifactInfo(Node node) {
        return objectMapper.convertValue(node.asMap(), ArtifactInfo.class);
    }

    private static class AggregateTree {
        private final ArtifactInfo artifactInfo;
        private final RelationProps relationProps;
        private final Map<Gav, AggregateTree> deps;

        AggregateTree(ArtifactInfo artifactInfo, RelationProps relationProps) {
            this.artifactInfo = artifactInfo;
            this.relationProps = relationProps;
            this.deps = new LinkedHashMap<>();
        }

        ArtifactTree toArtifactTree() {
            List<DependencyInfo> depsList = deps.values().stream()
                    .map(data -> new DependencyInfo(data.toArtifactTree(), data.relationProps.optional(), data.relationProps.scope()))
                    .toList();
            return new ArtifactTree(artifactInfo, depsList);
        }
    }

    private record RelationProps(Boolean optional, String scope) {}
}
