package dev.harrel.jarhell.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.License;
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
import java.time.LocalDateTime;
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
        try (var session = session()) {
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
                    .map(node -> new AggregateTree(toArtifactProps(node), 0))
                    .map(AggregateTree::toArtifactTree)
                    .sorted(Comparator.comparing(at -> at.artifactInfo().version()))
                    .toList();
        }
    }

    public boolean exists(Gav gav) {
        Map<String, Object> gavData = objectMapper.convertValue(gav, new TypeReference<>() {});
        gavData.computeIfAbsent("classifier", k -> "");
        try (var session = session()) {
            return session.executeRead(tx -> {
                Result res = tx.run(new Query("""
                        MATCH (root:Artifact)
                        WHERE
                            root.groupId = $props.groupId
                            AND root.artifactId = $props.artifactId
                            AND root.version = $props.version
                            AND root.classifier = $props.classifier
                        RETURN root""",
                        parameters("props", gavData))
                );
                return res.hasNext();
            });
        }
    }

    public List<Gav> findAllUnresolved(int limit, int unresolvedCountLimit) {
        try (var session = session()) {
            return session.executeRead(tx -> {
                Result res = tx.run("""
                        MATCH (root:Artifact)
                        WHERE
                            root.unresolved = true
                            AND coalesce(root.unresolvedCount, 1) < $unresolvedCountLimit
                        RETURN root.groupId, root.artifactId, root.version, root.classifier
                        LIMIT $limit""",
                        parameters("limit", limit, "unresolvedCountLimit", unresolvedCountLimit)
                );
                return res.list(rec -> new Gav(
                        rec.get("root.groupId").asString(),
                        rec.get("root.artifactId").asString(),
                        rec.get("root.version").asString(),
                        rec.get("root.classifier").asString()
                ));
            });
        }
    }

    public List<Gav> findAllEffectivelyUnresolved(int limit, int unresolvedCountLimit) {
        try (var session = session()) {
            return session.executeRead(tx -> {
                Result res = tx.run("""
                        MATCH (root:Artifact)
                        WHERE
                            root.effectiveUnresolvedDependencies > 0
                            AND coalesce(root.unresolvedCount, 1) < $unresolvedCountLimit
                        RETURN root.groupId, root.artifactId, root.version, root.classifier
                        LIMIT $limit""",
                        parameters("limit", limit, "unresolvedCountLimit", unresolvedCountLimit)
                );
                return res.list(rec -> new Gav(
                        rec.get("root.groupId").asString(),
                        rec.get("root.artifactId").asString(),
                        rec.get("root.version").asString(),
                        rec.get("root.classifier").asString()
                ));
            });
        }
    }

    public List<Gav> search(String token) {
        try (var session = session()) {
            return session.executeRead(tx -> {
                Result res = tx.run("""
                                MATCH (n:Artifact)
                                WHERE n.groupId CONTAINS $token
                                OR n.artifactId CONTAINS $token
                                RETURN DISTINCT n.groupId, n.artifactId
                                LIMIT 40""",
                        parameters("token", token));
                return res.list(r ->
                        new Gav(r.get("n.groupId").asString(), r.get("n.artifactId").asString(), "absent"));
            });
        }
    }

    public List<Gav> search(String groupId, String artifactId) {
        try (var session = session()) {
            return session.executeRead(tx -> {
                Result res = tx.run("""
                                MATCH (n:Artifact)
                                WHERE n.groupId CONTAINS $groupId
                                AND n.artifactId CONTAINS $artifactId
                                RETURN DISTINCT n.groupId, n.artifactId
                                LIMIT 40""",
                        parameters("groupId", groupId, "artifactId", artifactId));
                return res.list(r ->
                        new Gav(r.get("n.groupId").asString(), r.get("n.artifactId").asString(), "absent"));
            });
        }
    }

    public Optional<ArtifactTree> findResolved(Gav gav) {
        return find(gav, 0)
                .filter(at -> !Boolean.TRUE.equals(at.artifactInfo().unresolved()))
                .filter(at -> at.artifactInfo().effectiveValues().unresolvedDependencies() == 0);
    }

    public Optional<ArtifactTree> find(Gav gav, int depth) {
        Map<String, Object> gavData = objectMapper.convertValue(gav, new TypeReference<>() {});
        gavData.computeIfAbsent("classifier", k -> "");
        try (var session = session()) {
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
                    .collect(Collectors.toMap(Entity::elementId, n -> new AggregateTree(toArtifactProps(n), depth)));
            AggregateTree rootTree = new AggregateTree(toArtifactProps(rootNode), depth);
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

    public void saveArtifact(ArtifactInfo artifactInfo) {
        ArtifactProps artifactProps = toArtifactProps(artifactInfo);
        Map<String, Object> propsMap = objectMapper.convertValue(artifactProps, new TypeReference<>() {});
        propsMap.computeIfAbsent("classifier", _ -> "");
        try (var session = session()) {
            session.executeWriteWithoutResult(tx ->
                    tx.run("""
                                    MERGE (a:Artifact {groupId: $props.groupId, artifactId: $props.artifactId, version: $props.version, classifier: $props.classifier})
                                    WITH a, a.unresolvedCount AS unresolvedCount
                                    SET a = $props, a.analyzed = localdatetime()
                                    WITH a, unresolvedCount
                                    WHERE a.unresolved = true OR a.effectiveUnresolvedDependencies > 0
                                    SET a.unresolvedCount = coalesce(unresolvedCount, 0) + 1""",
                            parameters("props", propsMap))
            );
        }
    }

    public void saveDependencies(Gav parent, List<FlatDependency> deps) {
        try (var session = session()) {
            session.executeWriteWithoutResult(tx -> saveDependencies(tx, parent, deps));
        }
    }

    private void saveDependencies(TransactionContext tx, Gav parent, List<FlatDependency> deps) {
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

    /* Just because of bug: https://github.com/neo4j/neo4j-java-driver/issues/1601
    * I don't use bookmarks either way so maybe it's even better that way */
    private Session session() {
        return driver.session(SessionConfig.builder().withBookmarkManager(null).build());
    }

    private Gav toGav(ArtifactProps artifactProps) {
        return new Gav(artifactProps.groupId(), artifactProps.artifactId(), artifactProps.version(), artifactProps.classifier());
    }

    private Map<String, Object> toGavMap(Gav gav) {
        Map<String, Object> gavMap = objectMapper.convertValue(gav, new TypeReference<>() {});
        gavMap.computeIfAbsent("classifier", k -> "");
        return gavMap;
    }

    private ArtifactInfo toArtifactInfo(ArtifactProps artifactProps) {
        try {
            List<License> licenses = List.of();
            if (artifactProps.licenses() != null) {
                licenses = objectMapper.readValue(artifactProps.licenses(), new TypeReference<>() {});
            }
            List<LicenseType> licenseTypes = List.of();
            if (artifactProps.licenseTypes() != null) {
                licenseTypes = artifactProps.licenseTypes().stream().map(LicenseType::valueOf).toList();
            }

            ArtifactInfo.EffectiveValues effectiveValues = null;
            if (artifactProps.effectiveLicenseType() != null && artifactProps.effectiveLicenseTypes() != null) {
                LicenseType effectiveLicenseType = LicenseType.valueOf(artifactProps.effectiveLicenseType());
                List<Map.Entry<LicenseType, Long>> effectiveLicenseTypes = artifactProps.effectiveLicenseTypes().stream()
                        .map(entry -> entry.split(";"))
                        .map(entry -> Map.entry(LicenseType.valueOf(entry[0]), Long.valueOf(entry[1])))
                        .toList();
                effectiveValues = new ArtifactInfo.EffectiveValues(
                        artifactProps.effectiveRequiredDependencies(),
                        artifactProps.effectiveUnresolvedDependencies(),
                        artifactProps.effectiveOptionalDependencies(),
                        artifactProps.effectiveSize(),
                        artifactProps.effectiveBytecodeVersion(),
                        effectiveLicenseType,
                        effectiveLicenseTypes);
            }

            return new ArtifactInfo(artifactProps.groupId(), artifactProps.artifactId(), artifactProps.version(), artifactProps.classifier(),
                    artifactProps.unresolved(), artifactProps.unresolvedCount(), artifactProps.unresolvedReason(), artifactProps.created(),
                    artifactProps.packageSize(), artifactProps.bytecodeVersion(), artifactProps.packaging(), artifactProps.name(),
                    artifactProps.description(), artifactProps.url(), artifactProps.scmUrl(), artifactProps.issuesUrl(), artifactProps.inceptionYear(),
                    licenses, licenseTypes, artifactProps.classifiers(), effectiveValues, artifactProps.analyzed());
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
            List<String> licenseTypes = null;
            if (artifactInfo.licenseTypes() != null && !artifactInfo.licenseTypes().isEmpty()) {
                licenseTypes = artifactInfo.licenseTypes().stream().map(Enum::name).toList();
            }
            Integer effectiveDependencies = null;
            Integer effectiveUnresolvedDependencies = null;
            Integer effectiveOptionalDependencies = null;
            Long effectiveSize = null;
            String effectiveBytecodeVersion = null;
            String effectiveLicenseType = null;
            List<String> effectiveLicenseTypes = null;
            if (artifactInfo.effectiveValues() != null) {
                effectiveDependencies = artifactInfo.effectiveValues().requiredDependencies();
                effectiveUnresolvedDependencies = artifactInfo.effectiveValues().unresolvedDependencies();
                effectiveOptionalDependencies = artifactInfo.effectiveValues().optionalDependencies();
                effectiveSize = artifactInfo.effectiveValues().size();
                effectiveBytecodeVersion = artifactInfo.effectiveValues().bytecodeVersion();
                effectiveLicenseType = artifactInfo.effectiveValues().licenseType().name();
                effectiveLicenseTypes = artifactInfo.effectiveValues().licenseTypes().stream()
                        .map(entry -> "%s;%s".formatted(entry.getKey().name(), entry.getValue()))
                        .toList();
            }
            return new ArtifactProps(artifactInfo.groupId(), artifactInfo.artifactId(), artifactInfo.version(), artifactInfo.classifier(),
                    artifactInfo.unresolved(), artifactInfo.unresolvedCount(), artifactInfo.unresolvedReason(), artifactInfo.created(),
                    artifactInfo.packageSize(), artifactInfo.bytecodeVersion(), artifactInfo.packaging(), artifactInfo.name(),
                    artifactInfo.description(), artifactInfo.url(), artifactInfo.scmUrl(), artifactInfo.issuesUrl(),
                    artifactInfo.inceptionYear(), licenses, licenseTypes, artifactInfo.classifiers(), effectiveDependencies,
                    effectiveUnresolvedDependencies, effectiveOptionalDependencies, effectiveSize,
                    effectiveBytecodeVersion, effectiveLicenseType, effectiveLicenseTypes, null);
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
                                 Integer unresolvedCount,
                                 String unresolvedReason,
                                 LocalDateTime created,
                                 Long packageSize,
                                 String bytecodeVersion,
                                 String packaging,
                                 String name,
                                 String description,
                                 String url,
                                 String scmUrl,
                                 String issuesUrl,
                                 String inceptionYear,
                                 String licenses,
                                 List<String> licenseTypes,
                                 List<String> classifiers,
                                 Integer effectiveRequiredDependencies,
                                 Integer effectiveUnresolvedDependencies,
                                 Integer effectiveOptionalDependencies,
                                 Long effectiveSize,
                                 String effectiveBytecodeVersion,
                                 String effectiveLicenseType,
                                 List<String> effectiveLicenseTypes,
                                 LocalDateTime analyzed) {}

    private class AggregateTree {
        private final ArtifactProps artifactProps;
        private final int depth;
        private final SortedMap<Gav, AggregateTree> deps;
        private RelationProps relationProps;

        AggregateTree(ArtifactProps artifactProps, int depth) {
            this.artifactProps = artifactProps;
            this.depth = depth;
            this.deps = new TreeMap<>();
        }

        ArtifactTree toArtifactTree() {
            return toArtifactTree(Set.of(), 0);
        }

        private ArtifactTree toArtifactTree(Set<Gav> visited, int currentDepth) {
            Gav gav = toGav(artifactProps);
            List<DependencyInfo> depsList = null;
            if (!visited.contains(gav)) {
                Set<Gav> newVisited = new HashSet<>(visited);
                newVisited.add(gav);
                if (depth == -1 || currentDepth < depth) {
                    depsList = deps.values().stream()
                            .map(data -> new DependencyInfo(data.toArtifactTree(newVisited, currentDepth + 1), data.relationProps.optional(), data.relationProps.scope()))
                            .toList();
                }
            }
            return new ArtifactTree(toArtifactInfo(artifactProps), depsList);
        }
    }

    private record RelationProps(Boolean optional, String scope) {}
}
