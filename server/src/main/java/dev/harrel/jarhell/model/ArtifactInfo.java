package dev.harrel.jarhell.model;

import dev.harrel.jarhell.analyze.JarAnalyzer;
import dev.harrel.jarhell.model.descriptor.License;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ArtifactInfo(String groupId,
                           String artifactId,
                           String version,
                           String classifier,
                           Boolean fromMavenIndex,
                           Boolean unresolved,
                           Integer unresolvedCount,
                           String unresolvedReason,
                           LocalDateTime created,
                           Long packageSize,
                           String packaging,
                           String name,
                           String description,
                           String url,
                           String scmUrl,
                           String issuesUrl,
                           String inceptionYear,
                           List<License> licenses,
                           List<LicenseType> licenseTypes,
                           List<String> classifiers,
                           List<String> extensions,
                           JarAnalyzer.JarInfo jarInfo,
                           EffectiveValues effectiveValues,
                           LocalDateTime analyzed) {
    public static ArtifactInfo unresolved(Gav gav, String reason) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null, true, 1, reason,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    public static ArtifactInfo fromMavenIndex(Gav gav) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), true, true, 1, "initial-indexing",
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    public ArtifactInfo withEffectiveValues(EffectiveValues effectiveValues) {
        return new ArtifactInfo(groupId, artifactId, version, classifier, fromMavenIndex, unresolved, unresolvedCount, unresolvedReason, created, packageSize,
                packaging, name, description, url, scmUrl, issuesUrl, inceptionYear, licenses, licenseTypes, classifiers, extensions,
                jarInfo, effectiveValues, analyzed);
    }

    public record EffectiveValues(Integer requiredDependencies,
                                  Integer unresolvedDependencies,
                                  Integer optionalDependencies,
                                  Long size,
                                  BytecodeVersion bytecodeVersion,
                                  LicenseType licenseType,
                                  List<LicenseCount> licenseTypes) {
        public EffectiveValues {
            Objects.requireNonNull(licenseType);
            Objects.requireNonNull(licenseTypes);
        }
    }
}
