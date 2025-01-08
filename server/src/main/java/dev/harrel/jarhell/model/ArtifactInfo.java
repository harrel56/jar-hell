package dev.harrel.jarhell.model;

import dev.harrel.jarhell.model.descriptor.License;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ArtifactInfo(String groupId,
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
                           List<License> licenses,
                           List<LicenseType> licenseTypes,
                           List<String> classifiers,
                           EffectiveValues effectiveValues,
                           LocalDateTime analyzed) {
    public static ArtifactInfo unresolved(Gav gav, String reason) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), true, 1, reason,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    public ArtifactInfo withEffectiveValues(EffectiveValues effectiveValues) {
        return new ArtifactInfo(groupId, artifactId, version, classifier, unresolved, unresolvedCount, unresolvedReason, created, packageSize, bytecodeVersion,
                packaging, name, description, url, scmUrl, issuesUrl, inceptionYear, licenses, licenseTypes, classifiers, effectiveValues, analyzed);
    }

    public record EffectiveValues(Integer requiredDependencies,
                                  Integer unresolvedDependencies,
                                  Integer optionalDependencies,
                                  Long size,
                                  String bytecodeVersion,
                                  LicenseType licenseType,
                                  List<Map.Entry<LicenseType, Long>> licenseTypes) {
        public EffectiveValues {
            Objects.requireNonNull(licenseType);
            Objects.requireNonNull(licenseTypes);
        }
    }
}
