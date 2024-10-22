package dev.harrel.jarhell.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.Licence;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

@Singleton
public class ArtifactRepository {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepository.class);

    private final Driver driver;
    private final ObjectMapper objectMapper;

    public ArtifactRepository(Driver driver, ObjectMapper objectMapper) {
        this.driver = driver;
        this.objectMapper = objectMapper;
    }

    public List<ArtifactTree> findAllVersions(String groupId, String artifactId, String classifier) {
        try (var session = driver.session()) {
            SummarizedResult result = session.executeRead(tx -> {
                Result res = tx.run(new Query("""
                                MATCH (root:Artifact)
                                WHERE
                                    root.groupId = $groupId
                                    AND root.artifactId = $artifactId
                                    AND root.classifier = $classifier
                                RETURN root
                                """,
                        parameters(
                                "groupId", groupId,
                                "artifactId", artifactId,
                                "classifier", classifier == null ? "" : classifier)
                        )
                );
                return new SummarizedResult(res.list(), res.consume());
            });

            logger.info("Querying for all versions of artifact [{}:{}(classifier: {})] - size: {}, availableAfter: {}ms, consumedAfter: {}ms",
                    groupId,
                    artifactId,
                    classifier,
                    result.records().size(),
                    result.summary().resultAvailableAfter(TimeUnit.MILLISECONDS),
                    result.summary().resultConsumedAfter(TimeUnit.MILLISECONDS)
            );

            return result.records().stream()
                    .map(rec -> rec.get("root").asNode())
                    .map(node -> new AggregateTree(toArtifactProps(node)))
                    .map(AggregateTree::toArtifactTree)
                    .sorted(Comparator.comparing(at -> at.artifactInfo().version()))
                    .toList();
        }
    }

    public Optional<ArtifactTree> find(Gav gav) {
        return find(gav, -1);
    }

    public Optional<ArtifactTree> find(Gav gav, int depth) {
        Map<String, Object> gavData = objectMapper.convertValue(gav, new TypeReference<>() {});
        gavData.computeIfAbsent("classifier", k -> "");
        try (var session = driver.session()) {
            SummarizedResult result = session.executeRead(tx -> {
                Result res = tx.run(new Query("""
                        MATCH (root:Artifact)
                        WHERE
                            root.groupId = $props.groupId
                            AND root.artifactId = $props.artifactId
                            AND root.version = $props.version
                            AND root.classifier = $props.classifier
                        CALL apoc.path.subgraphAll(root, {
                            relationshipFilter: "DEPENDS_ON>",
                            minLevel: 0,
                            maxLevel: $depth
                        })
                        YIELD nodes, relationships
                        RETURN root, nodes, relationships""",
                        parameters("props", gavData, "depth", depth))
                );
                return new SummarizedResult(res.list(), res.consume());
            });

            logger.info("Querying for artifact [{}] - availableAfter: {}ms, consumedAfter: {}ms",
                    gav,
                    result.summary().resultAvailableAfter(TimeUnit.MILLISECONDS),
                    result.summary().resultConsumedAfter(TimeUnit.MILLISECONDS)
            );
            if (result.records().isEmpty()) {
                return Optional.empty();
            } else if (result.records().size() > 1) {
                throw new IllegalArgumentException("Query returned too many records");
            }
            var rec = result.records().getFirst();

            Node rootNode = rec.get("root").asNode();
            Map<String, AggregateTree> nodes = rec.get("nodes").asList(Value::asEntity).stream()
                    .collect(Collectors.toMap(Entity::elementId, n -> new AggregateTree(toArtifactProps(n))));
            AggregateTree rootTree = new AggregateTree(toArtifactProps(rootNode));
            nodes.put(rootNode.elementId(), rootTree);

            List<Relationship> relations = rec.get("relationships").asList(Value::asRelationship);
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
        propsMap.computeIfAbsent("classifier", k -> "");

        List<FlatDependency> deps = artifactTree.dependencies().stream()
                .map(dep -> new FlatDependency(toGav(dep.artifact().artifactInfo()), dep.optional(), dep.scope()))
                .toList();
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> {
                tx.run(new Query("CREATE (a:Artifact $props) SET a.created = datetime()", parameters("props", propsMap)));
                saveDependency(tx, toGav(artifactTree.artifactInfo()), deps);
            });
        }
    }

    public void saveDependency(Gav parent, FlatDependency dep) {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> saveDependency(tx, parent, List.of(dep)));
        }
    }

    private void saveDependency(TransactionContext tx, Gav parent, List<FlatDependency> deps) {
        Map<String, Object> parentGavMap = toGavMap(parent);
        List<Map<String, Map<String, Object>>> dependencies = deps.stream().map(dep -> {
            Map<String, Object> depGavMap = toGavMap(dep.gav());
            Map<String, Object> depProps = Map.of("optional", dep.optional(), "scope", dep.scope());
            return Map.of("parentGav", parentGavMap, "depGav", depGavMap, "depProps", depProps);
        }).toList();
        tx.run(new Query("""
                UNWIND $dependencies AS dep
                MATCH (a:Artifact), (d:Artifact)
                WHERE
                    a.groupId = dep.parentGav.groupId
                    AND a.artifactId = dep.parentGav.artifactId
                    AND a.version = dep.parentGav.version
                    AND a.classifier = dep.parentGav.classifier
                    AND d.groupId = dep.depGav.groupId
                    AND d.artifactId = dep.depGav.artifactId
                    AND d.version = dep.depGav.version
                    AND d.classifier = dep.depGav.classifier
                MERGE (a)-[:DEPENDS_ON {optional: dep.depProps.optional, scope: dep.depProps.scope}]->(d)""",
                parameters("dependencies", dependencies)));
    }

    private Gav toGav(ArtifactProps artifactProps) {
        return new Gav(artifactProps.groupId(), artifactProps.artifactId(), artifactProps.version(), artifactProps.classifier());
    }

    private Gav toGav(ArtifactInfo artifactInfo) {
        return new Gav(artifactInfo.groupId(), artifactInfo.artifactId(), artifactInfo.version(), artifactInfo.classifier());
    }

    private Map<String, Object> toGavMap(Gav gav) {
        Map<String, Object> gavMap = objectMapper.convertValue(gav, new TypeReference<>() {});
        gavMap.computeIfAbsent("classifier", k -> "");
        return gavMap;
    }

    private ArtifactInfo toArtifactInfo(ArtifactProps artifactProps) {
        try {
            List<Licence> licences = List.of();
            if (artifactProps.licenses() != null) {
                licences = objectMapper.readValue(artifactProps.licenses(), new TypeReference<>() {});
            }
            return new ArtifactInfo(artifactProps.groupId(), artifactProps.artifactId(), artifactProps.version(), artifactProps.classifier(),
                    artifactProps.unresolved(), artifactProps.packageSize(), artifactProps.totalSize(), artifactProps.bytecodeVersion(),
                    artifactProps.packaging(), artifactProps.name(), artifactProps.description(), artifactProps.url(),
                    artifactProps.inceptionYear(), licences, artifactProps.classifiers(), artifactProps.created());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ArtifactProps toArtifactProps(MapAccessor node) {
        return objectMapper.convertValue(node.asMap(), ArtifactProps.class);
    }

    private ArtifactProps toArtifactProps(ArtifactInfo artifactInfo) {
        try {
            String licenses = null;
            if (artifactInfo.licenses() != null && !artifactInfo.licenses().isEmpty()) {
                licenses = objectMapper.writeValueAsString(artifactInfo.licenses());
            }
            return new ArtifactProps(artifactInfo.groupId(), artifactInfo.artifactId(), artifactInfo.version(), artifactInfo.classifier(),
                    artifactInfo.unresolved(), artifactInfo.packageSize(), artifactInfo.totalSize(), artifactInfo.bytecodeVersion(),
                    artifactInfo.packaging(), artifactInfo.name(), artifactInfo.description(), artifactInfo.url(),
                    artifactInfo.inceptionYear(), licenses, artifactInfo.classifiers(), null);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record SummarizedResult(List<Record> records, ResultSummary summary) {}

    private record ArtifactProps(String groupId,
                                 String artifactId,
                                 String version,
                                 String classifier,
                                 Boolean unresolved,
                                 Long packageSize,
                                 Long totalSize,
                                 String bytecodeVersion,
                                 String packaging,
                                 String name,
                                 String description,
                                 String url,
                                 String inceptionYear,
                                 String licenses,
                                 List<String> classifiers,
                                 ZonedDateTime created) {}

    private class AggregateTree {
        private final ArtifactProps artifactProps;
        private final SortedMap<Gav, AggregateTree> deps;
        private RelationProps relationProps;

        AggregateTree(ArtifactProps artifactProps) {
            this.artifactProps = artifactProps;
            this.deps = new TreeMap<>();
        }

        ArtifactTree toArtifactTree() {
            return toArtifactTree(Set.of());
        }

        private ArtifactTree toArtifactTree(Set<Gav> visited) {
            Gav gav = toGav(artifactProps);
            List<DependencyInfo> depsList = null;
            if (!visited.contains(gav)) {
                Set<Gav> newVisited = new HashSet<>(visited);
                newVisited.add(gav);
                depsList = deps.values().stream()
                        .map(data -> new DependencyInfo(data.toArtifactTree(newVisited), data.relationProps.optional(), data.relationProps.scope()))
                        .toList();
            }
            return new ArtifactTree(toArtifactInfo(artifactProps), depsList);
        }
    }

    private record RelationProps(Boolean optional, String scope) {}
}
