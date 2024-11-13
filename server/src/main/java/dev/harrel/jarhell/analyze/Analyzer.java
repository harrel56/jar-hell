package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
class Analyzer {
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);

    private final MavenRunner mavenRunner;
    private final MavenApiClient mavenApiClient;
    private final PackageAnalyzer packageAnalyzer;

    Analyzer(MavenRunner mavenRunner, MavenApiClient mavenApiClient, PackageAnalyzer packageAnalyzer) {
        this.mavenRunner = mavenRunner;
        this.mavenApiClient = mavenApiClient;
        this.packageAnalyzer = packageAnalyzer;
    }

    public boolean checkIfArtifactExists(Gav gav) {
        return mavenApiClient.checkIfArtifactExists(gav);
    }

    public CollectedDependencies analyzeDeps(Gav gav) {
        return mavenRunner.collectDependencies(gav);
    }

    public ArtifactInfo analyzePackage(Gav gav) {
        try {
            FilesInfo filesInfo = mavenApiClient.fetchFilesInfo(gav);
            DescriptorInfo descriptorInfo = mavenRunner.resolveDescriptor(gav);
            PackageInfo packageInfo = packageAnalyzer.analyzePackage(gav, filesInfo, descriptorInfo.packaging());

            return createArtifactInfo(gav, filesInfo, packageInfo, descriptorInfo);
        } catch (Exception e) {
            logger.warn("Failed to analyze artifact: {}, marking it as unresolved", gav, e);
            return ArtifactInfo.unresolved(gav);
        }
    }

    public ArtifactInfo.EffectiveValues computeEffectiveValues(ArtifactInfo info, List<DependencyInfo> partialDeps) {
        if (Boolean.TRUE.equals(info.unresolved())) {
            return null;
        }

        List<ArtifactInfo> requiredDeps = partialDeps.stream()
                .filter(d -> !d.optional())
                .map(DependencyInfo::artifact)
                .map(ArtifactTree::artifactInfo)
                .toList();
        int optionalDeps = partialDeps.size() - requiredDeps.size();
        int unresolvedDeps = Math.toIntExact(requiredDeps.stream().filter(dep -> Boolean.TRUE.equals(dep.unresolved())).count());
        long totalSize = Objects.requireNonNullElse(info.packageSize(), 0L) +
                requiredDeps.stream()
                        .mapToLong(a -> Objects.requireNonNullElse(a.packageSize(), 0L))
                        .sum();
        String bytecodeVersion = Stream.concat(Stream.of(info), requiredDeps.stream())
                .map(ArtifactInfo::bytecodeVersion)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new ArtifactInfo.EffectiveValues(requiredDeps.size(), unresolvedDeps, optionalDeps, totalSize, bytecodeVersion);
    }

    // todo: maybe actually save excluded and conflicted deps
    public TraversalOutput adjustArtifactTree(ArtifactTree artifactTree, List<ArtifactTree> allDependenciesList) {
        Map<Ga, ArtifactTree> allDeps = allDependenciesList.stream()
                .collect(Collectors.toMap(at -> new Ga(at.artifactInfo().groupId(), at.artifactInfo().artifactId()), Function.identity()));

        Set<Ga> excluded = new HashSet<>();
        Set<Gav> conflicted = new HashSet<>();
        traverseDeps(artifactTree, allDeps, excluded, conflicted, new HashSet<>());
        return new TraversalOutput(artifactTree, excluded, conflicted);
    }

    private ArtifactInfo createArtifactInfo(Gav gav, FilesInfo filesInfo, PackageInfo packageInfo, DescriptorInfo descriptorInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null, filesInfo.created(),
                packageInfo.size(), packageInfo.bytecodeVersion(), descriptorInfo.packaging(),
                descriptorInfo.name(), descriptorInfo.description(), descriptorInfo.url(),
                descriptorInfo.scmUrl(), descriptorInfo.issuesUrl(), descriptorInfo.inceptionYear(),
                descriptorInfo.licences(), List.copyOf(filesInfo.classifiers()), null, null);
    }

    private void traverseDeps(ArtifactTree at,
                              Map<Ga, ArtifactTree> allDeps,
                              Set<Ga> excluded,
                              Set<Gav> conflicted,
                              Set<Gav> visited) {
        Gav gav = treeToGav(at);
        if (visited.contains(gav)) {
            return;
        }

        visited.add(gav);

        // handle excluded deps
        at.dependencies().stream()
                .map(di -> treeToGa(di.artifact()))
                .filter(ga -> !allDeps.containsKey(ga))
                .forEach(excluded::add);
        at.dependencies().removeIf(di -> excluded.contains(treeToGa(di.artifact())));

        // handle conflicted deps
        at.dependencies().stream()
                .filter(di -> {
                    Ga ga = treeToGa(di.artifact());
                    var depAt = allDeps.get(ga);
                    return !di.artifact().artifactInfo().version().equals(depAt.artifactInfo().version());
                })
                .map(di -> treeToGav(di.artifact()))
                .forEach(conflicted::add);
        for (int i = 0; i < at.dependencies().size(); i++) {
            DependencyInfo di = at.dependencies().get(i);
            Ga ga = treeToGa(di.artifact());
            var depAt = allDeps.get(ga);
            if (!di.artifact().artifactInfo().version().equals(depAt.artifactInfo().version())) {
                at.dependencies().set(i, new DependencyInfo(depAt, di.optional(), di.scope()));
            }
        }
    }

    private static Gav treeToGav(ArtifactTree at) {
        ArtifactInfo info = at.artifactInfo();
        return new Gav(info.groupId(), info.artifactId(), info.version(), info.classifier());
    }

    private static Ga treeToGa(ArtifactTree at) {
        return new Ga(at.artifactInfo().groupId(), at.artifactInfo().artifactId());
    }

    public record TraversalOutput(ArtifactTree artifactTree, Set<Ga> excluded, Set<Gav> conflicted) {}

    private record Ga(String groupId, String artifactId) {}
}

