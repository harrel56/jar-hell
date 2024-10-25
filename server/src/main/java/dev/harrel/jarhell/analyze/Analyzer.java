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
            return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier());
        }
    }

    public TraversalOutput adjustArtifactTree(ArtifactTree artifactTree, List<ArtifactTree> allDependenciesList) {
        Map<Ga, ArtifactTree> allDeps = allDependenciesList.stream()
                .collect(Collectors.toMap(at -> new Ga(at.artifactInfo().groupId(), at.artifactInfo().artifactId()), Function.identity()));

        Set<Ga> excluded = new HashSet<>();
        Set<Gav> conflicted = new HashSet<>();
        traverseDeps(artifactTree, allDeps, excluded, conflicted, new HashSet<>());
        return new TraversalOutput(artifactTree, excluded, conflicted);
    }

    public Long calculateTotalSize(ArtifactTree at) {
        if (at.artifactInfo().packageSize() == null) {
            return null;
        }
        return traverseTotalSize(at, new HashSet<>());
    }

    private ArtifactInfo createArtifactInfo(Gav gav, FilesInfo filesInfo, PackageInfo packageInfo, DescriptorInfo descriptorInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null,
                packageInfo.size(), null, packageInfo.bytecodeVersion(), descriptorInfo.packaging(),
                descriptorInfo.name(), descriptorInfo.description(), descriptorInfo.url(), descriptorInfo.inceptionYear(),
                descriptorInfo.licences(), List.copyOf(filesInfo.classifiers()), null);
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

    private long traverseTotalSize(ArtifactTree at, Set<Gav> visited) {
        Gav gav = treeToGav(at);
        if (visited.contains(gav)) {
            return 0;
        }

        Long size = Optional.ofNullable(at.artifactInfo().packageSize()).orElse(0L);
        return size + at.dependencies().stream()
                .filter(d -> !Boolean.TRUE.equals(d.optional()))
                .map(DependencyInfo::artifact)
                .mapToLong(d -> traverseTotalSize(d, visited))
                .sum();
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

