package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;
import dev.harrel.jarhell.model.descriptor.Licence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.net.URI;
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
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null, packageInfo.created(),
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

    enum LicenseType {
        APACHE_2_0(lowercaseSet(
                "Apache Software 2.0",
                "Apache Software 2",
                "Apache 2.0",
                "Apache 2",
                "AL 2.0",
                "AL 2",
                "AL2",
                "AL2.0"
        ), Set.of()),
        BSD_0(lowercaseSet(
                "0BSD",
                "BSD0",
                "BSD 0",
                "Zero Clause BSD",
                "BSD Zero Clause",
                "0 Clause BSD",
                "BSD 0 Clause"
        ), Set.of()),
        BSD_1(lowercaseSet(
                "BSD1",
                "BSD 1",
                "1 Clause BSD",
                "BSD 1 Clause"
        ), Set.of()),
        BSD_2(lowercaseSet(
                "Berkeley Software Distribution",
                "BSD",
                "BSD2",
                "BSD 2",
                "2 Clause BSD",
                "BSD 2 Clause",
                "Simplified BSD",
                "FreeBSD",
                "Free BSD"
        ), Set.of()),
        BSD_3(lowercaseSet(
                "BSD3",
                "BSD 3",
                "3 Clause BSD",
                "BSD 3 Clause",
                "NewBSD",
                "New BSD",
                "Modified BSD",
                "Revised BSD"
        ), Set.of()),
        CC0_1(lowercaseSet(
                "CC0",
                "CC0 1",
                "CC0 1.0",
                "Creative Commons 1.0",
                "Creative Commons 1",
                "Creative Commons"
        ), Set.of()),
        CDDL_1(lowercaseSet(
                "CDDL",
                "CDDL1",
                "CDDL1.0",
                "CDDL 1",
                "Common Development and Distribution 1.0",
                "Common Development and Distribution 1",
                "Common Development and Distribution"
        ), Set.of()),
        CPL_1(lowercaseSet(
                "CPL",
                "CPL1",
                "CPL1.0",
                "CPL 1",
                "Common Public 1.0",
                "Common Public 1",
                "Common Public"
        ), Set.of()),
        GPL_2(lowercaseSet(
                "GPL2",
                "GPL2.0",
                "GPL 2.0",
                "GPL 2",
                "GNU GPL 2.0",
                "GNU GPL 2",
                "GNU GPL2",
                "GNU General Public 2.0",
                "GNU General Public 2",
                "General Public 2.0",
                "General Public 2"
        ), Set.of()),
        GPL_3(lowercaseSet(
                "GPL3",
                "GPL3.0",
                "GPL 3.0",
                "GPL 3",
                "GNU GPL 3.0",
                "GNU GPL 3",
                "GNU GPL3",
                "GNU General Public 3.0",
                "GNU General Public 3",
                "General Public 3.0",
                "General Public 3"
        ), Set.of()),
        AGPL_3(lowercaseSet(
                "AGPL3",
                "AGPL3.0",
                "AGPL 3.0",
                "AGPL 3",
                "AGPL",
                "GNU AGPL 3.0",
                "GNU AGPL 3",
                "GNU AGPL3",
                "GNU AGPL",
                "Affero GNU General Public 3.0",
                "Affero GNU General Public 3",
                "Affero GNU General Public",
                "GNU Affero General Public 3.0",
                "GNU Affero General Public 3",
                "GNU Affero General Public"
        ), Set.of()),
        LGPL_2(lowercaseSet(
                "LGPL2",
                "LGPL2.1",
                "LGPL 2.1",
                "LGPL 2",
                "GNU LGPL 2.1",
                "GNU LGPL 2",
                "GNU LGPL2",
                "GNU Lesser General Public 2.1",
                "GNU Lesser General Public 2",
                "Lesser General Public 2.1",
                "Lesser General Public 2"
        ), Set.of()),
        LGPL_3(lowercaseSet(
                "LGPL3",
                "LGPL3.0",
                "LGPL 3.0",
                "LGPL 3",
                "GNU LGPL 3.0",
                "GNU LGPL 3",
                "GNU LGPL3",
                "GNU Lesser General Public 3.0",
                "GNU Lesser General Public 3",
                "Lesser General Public 3.0",
                "Lesser General Public 3"
        ), Set.of()),
        MIT(lowercaseSet(
                "MIT"
        ), Set.of()),
        MIT0(lowercaseSet(
                "MIT0",
                "MIT 0",
                "MIT No Attribution"
        ), Set.of()),
        MPL_2(lowercaseSet(
                "Mozilla Public 2.0",
                "Mozilla Public 2",
                "MPL",
                "MPL2",
                "MPL2.0",
                "MPL 2",
                "MPL 2.0"
        ), Set.of()),
        ISC(lowercaseSet(
                "ISC"
        ), Set.of()),
        ICU(lowercaseSet(
                "ICU"
        ), Set.of()),
        EPL_1(lowercaseSet(
                "Eclipse Public",
                "Eclipse Public 1.0",
                "Eclipse Public 1",
                "EPL",
                "EPL1",
                "EPL1.0",
                "EPL 1.0",
                "EPL 1"
        ), Set.of()),
        EPL_2(lowercaseSet(
                "Eclipse Public 2.0",
                "Eclipse Public 2",
                "EPL2",
                "EPL2.0",
                "EPL 2.0",
                "EPL 2"
        ), Set.of()),
        SSPL_1(lowercaseSet(
                "SSPL",
                "SSPL1",
                "SSPL1.0",
                "SSPL 1",
                "SSPL 1.0",
                "Server Side Public 1.0",
                "Server Side Public 1",
                "Server Side Public"
        ), Set.of()),
        UNLICENSE(lowercaseSet(
                "Unlicense",
                "Public Domain",
                "PD"
        ), Set.of()),
        ZLIB(lowercaseSet(
                "zlib",
                "zlib/libpng",
                "libpng",
                "lib png"
        ), Set.of());

        private final Set<String> names;
        private final Set<URI> uris;

        LicenseType(Set<String> names, Set<URI> uris) {
            this.names = names;
            this.uris = uris;
        }

        /// # Input normalization before comparison:
        /// - lowercase
        /// - convert '-', '_' to spaces
        /// - get rid of 'v' if preceding a number or as single letter
        /// - remove 'the', 'version', 'license'
        /// - remove commas
        /// - remove trailing periods
        /// - remove all content inside '()'
        /// - StringUtils.normalizeSpace() - to dedup spaces
        /// - trim()
        public boolean matches(Licence licence) {
            return false; //todo
        }

        private static Set<String> lowercaseSet(String... items) {
            return Arrays.stream(items).map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
        }
    }
}

