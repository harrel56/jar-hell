package dev.harrel.jarhell.handler;

import dev.harrel.jarhell.repo.ArtifactRepository;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class PackagesHandler implements Handler {
    private final ArtifactRepository artifactRepository;

    public PackagesHandler(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        String coordinate = ctx.pathParam("coordinate");
        Gav gav = Gav.fromCoordinate(coordinate);

        ArtifactTree artifactTree = artifactRepository.find(gav)
                .orElseThrow(() -> new ResourceNotFoundException("Package with coordinates [%s] not found".formatted(gav)));

        ctx.json(artifactTree);
    }
}
