package dev.harrel.jarhell.model;

import dev.harrel.jarhell.model.descriptor.Licence;

import java.util.List;

public record ArtifactInfo(String groupId,
                           String artifactId,
                           String version,
                           String classifier,
                           Boolean unresolved,
                           Long packageSize,
                           String bytecodeVersion,
                           String packaging,
                           String name,
                           String description,
                           String url,
                           String inceptionYear,
                           List<Licence> licenses) {
    public ArtifactInfo(String groupId, String artifactId, String version, String classifier) {
        this(groupId, artifactId, version, classifier, true, null, null, null, null, null, null, null, null);
    }
}
