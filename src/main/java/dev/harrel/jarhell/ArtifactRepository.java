package dev.harrel.jarhell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

public class ArtifactRepository {
    private final Driver driver;
    private final ObjectMapper objectMapper;

    public ArtifactRepository(Driver driver, ObjectMapper objectMapper) {
        this.driver = driver;
        this.objectMapper = objectMapper;
    }

    public Optional<ArtifactTree> find(Gav gav) {
        Map<String, Object> gavData = objectMapper.convertValue(gav, new TypeReference<>() {
        });
        try (var session = driver.session()) {
            List<org.neo4j.driver.Record> records = session.executeRead(tx -> tx.run(new Query("""
                            MATCH (root:Artifact {groupId: $props.groupId, artifactId: $props.artifactId, version: $props.version})-[rel:DEPENDS_ON*0..]->(dep:Artifact)
                            UNWIND rel AS flatRel
                            WITH root, flatRel, dep ORDER BY dep.groupId, dep.artifactId, dep.version
                            RETURN COLLECT(DISTINCT root) AS root, COLLECT(DISTINCT flatRel) AS relations, COLLECT(DISTINCT dep) AS deps""",
                            parameters("props", gavData))
                    ).list()
            );

            if (records.size() > 1) {
                throw new IllegalArgumentException("Too many results");
            }
            if (records.isEmpty()) {
                return Optional.empty();
            }

            Record record = records.get(0);
            Node rootNode = record.get("root").get(0).asNode();
            Map<String, AggregateTree> nodes = record.get("deps").asList(Value::asEntity).stream()
                    .collect(Collectors.toMap(Entity::elementId, n -> new AggregateTree(toArtifactInfo(n))));
            AggregateTree rootTree = new AggregateTree(toArtifactInfo(rootNode));
            nodes.put(rootNode.elementId(), rootTree);

            List<Relationship> relations = record.get("relations").asList(Value::asRelationship);
            for (Relationship relation : relations) {
                AggregateTree start = nodes.get(relation.startNodeElementId());
                AggregateTree end = nodes.get(relation.endNodeElementId());

                end.relationProps = objectMapper.convertValue(relation.asMap(), RelationProps.class);

                Gav endGav = toGav(end.artifactInfo);
                start.deps.put(endGav, end);
            }

            return Optional.of(rootTree.toArtifactTree());
        }
    }

    public void save(ArtifactTree artifactTree) {
        Map<String, Object> propsMap = objectMapper.convertValue(artifactTree.artifactInfo(), new TypeReference<>() {
        });
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

    private ArtifactInfo toArtifactInfo(MapAccessor node) {
        return objectMapper.convertValue(node.asMap(), ArtifactInfo.class);
    }

    private static class AggregateTree {
        private final ArtifactInfo artifactInfo;
        private final Map<Gav, AggregateTree> deps;
        private RelationProps relationProps;

        AggregateTree(ArtifactInfo artifactInfo) {
            this.artifactInfo = artifactInfo;
            this.deps = new LinkedHashMap<>();
        }

        ArtifactTree toArtifactTree() {
            List<DependencyInfo> depsList = deps.values().stream()
                    .map(data -> new DependencyInfo(data.toArtifactTree(), data.relationProps.optional(), data.relationProps.scope()))
                    .toList();
            return new ArtifactTree(artifactInfo, depsList);
        }
    }

    private record RelationProps(Boolean optional, String scope) {
    }
}
