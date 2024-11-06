package dev.harrel.jarhell.model;

import dev.harrel.jarhell.model.descriptor.Licence;

import java.time.LocalDateTime;
import java.util.List;

public record ArtifactInfo(String groupId,
                           String artifactId,
                           String version,
                           String classifier,
                           Boolean unresolved,
                           LocalDateTime created,
                           Long packageSize,
                           String bytecodeVersion,
                           String packaging,
                           String name,
                           String description,
                           String url,
                           String inceptionYear,
                           List<Licence> licenses,
                           List<String> classifiers,
                           EffectiveValues effectiveValues,
                           LocalDateTime analyzed) {
    public static ArtifactInfo unresolved(Gav gav) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), true, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public ArtifactInfo withEffectiveValues(EffectiveValues effectiveValues) {
        return new ArtifactInfo(groupId, artifactId, version, classifier, unresolved, created, packageSize, bytecodeVersion,
                packaging, name, description, url, inceptionYear, licenses, classifiers, effectiveValues, analyzed);
    }

    public record EffectiveValues(Integer requiredDependencies,
                                  Integer unresolvedDependencies,
                                  Integer optionalDependencies,
                                  Long size,
                                  String bytecodeVersion) {}
}
