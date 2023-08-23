package dev.harrel.jarhell.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.DependencyInfo;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.descriptor.Licence;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.io.UncheckedIOException;
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
            org.neo4j.driver.Record rec = session.executeRead(tx -> tx.run(new Query("""
                            MATCH (root:Artifact {groupId: $props.groupId, artifactId: $props.artifactId, version: $props.version})-[rel:DEPENDS_ON*0..]->(dep:Artifact)
                            UNWIND
                                CASE
                                    WHEN rel = [] THEN [null]
                                    ELSE rel
                                END AS flatRel
                            WITH root, flatRel, dep ORDER BY dep.groupId, dep.artifactId, dep.version
                            RETURN COLLECT(DISTINCT root) AS root, COLLECT(DISTINCT flatRel) AS relations, COLLECT(DISTINCT dep) AS deps""",
                            parameters("props", gavData))
                    ).single()
            );

            if (rec.get("root").isEmpty()) {
                return Optional.empty();
            }

            Node rootNode = rec.get("root").get(0).asNode();
            Map<String, AggregateTree> nodes = rec.get("deps").asList(Value::asEntity).stream()
                    .collect(Collectors.toMap(Entity::elementId, n -> new AggregateTree(toArtifactProps(n))));
            AggregateTree rootTree = new AggregateTree(toArtifactProps(rootNode));
            nodes.put(rootNode.elementId(), rootTree);

            List<Relationship> relations = rec.get("relations").asList(Value::asRelationship);
            for (Relationship relation : relations) {
                AggregateTree start = nodes.get(relation.startNodeElementId());
                AggregateTree end = nodes.get(relation.endNodeElementId());

                end.relationProps = objectMapper.convertValue(relation.asMap(), RelationProps.class);

                Gav endGav = toGav(end.artifactProps);
                start.deps.put(endGav, end);
            }

            return Optional.of(rootTree.toArtifactTree());
        }
    }

    public void save(ArtifactTree artifactTree) {
        ArtifactProps artifactProps = toArtifactProps(artifactTree.artifactInfo());
        Map<String, Object> propsMap = objectMapper.convertValue(artifactProps, new TypeReference<>() {});
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(new Query("CREATE (a:Artifact $props)",
                            parameters("props", propsMap))
                    )
            );

            Map<String, Object> parentGav = toGavMap(artifactTree.artifactInfo());
            artifactTree.dependencies().forEach(dep -> {
                        Map<String, Object> depProps = Map.of("optional", dep.optional(), "scope", dep.scope());
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

    private Gav toGav(ArtifactProps artifactProps) {
        return new Gav(artifactProps.groupId(), artifactProps.artifactId(), artifactProps.version());
    }

    private Map<String, Object> toGavMap(ArtifactInfo info) {
        return Map.of(
                "groupId", info.groupId(),
                "artifactId", info.artifactId(),
                "version", info.version()
        );
    }

    private ArtifactInfo toArtifactInfo(ArtifactProps artifactProps) {
        try {
            List<Licence> licences = objectMapper.readValue(artifactProps.licenses(), new TypeReference<>() {});
            return new ArtifactInfo(artifactProps.groupId(), artifactProps.artifactId(), artifactProps.version(), artifactProps.packageSize(),
                    artifactProps.bytecodeVersion(), artifactProps.packaging(), artifactProps.name(), artifactProps.description(), artifactProps.url(),
                    artifactProps.inceptionYear(), licences);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ArtifactProps toArtifactProps(MapAccessor node) {
        return objectMapper.convertValue(node.asMap(), ArtifactProps.class);
    }

    private ArtifactProps toArtifactProps(ArtifactInfo artifactInfo) {
        try {
            String licenses = objectMapper.writeValueAsString(artifactInfo.licenses());
            return new ArtifactProps(artifactInfo.groupId(), artifactInfo.artifactId(), artifactInfo.version(), artifactInfo.packageSize(),
                    artifactInfo.bytecodeVersion(), artifactInfo.packaging(), artifactInfo.name(), artifactInfo.description(), artifactInfo.url(),
                    artifactInfo.inceptionYear(), licenses);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record ArtifactProps(String groupId,
                                 String artifactId,
                                 String version,
                                 Long packageSize,
                                 String bytecodeVersion,
                                 String packaging,
                                 String name,
                                 String description,
                                 String url,
                                 String inceptionYear,
                                 String licenses) {}

    private class AggregateTree {
        private final ArtifactProps artifactProps;
        private final Map<Gav, AggregateTree> deps;
        private RelationProps relationProps;

        AggregateTree(ArtifactProps artifactProps) {
            this.artifactProps = artifactProps;
            this.deps = new LinkedHashMap<>();
        }

        ArtifactTree toArtifactTree() {
            List<DependencyInfo> depsList = deps.values().stream()
                    .map(data -> new DependencyInfo(data.toArtifactTree(), data.relationProps.optional(), data.relationProps.scope()))
                    .toList();
            return new ArtifactTree(toArtifactInfo(artifactProps), depsList);
        }
    }

    private record RelationProps(Boolean optional, String scope) {}
}
