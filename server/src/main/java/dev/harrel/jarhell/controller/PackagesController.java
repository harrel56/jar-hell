package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;

@Controller("/api/v1/packages")
public class PackagesController {
    private final ArtifactRepository artifactRepository;

    public PackagesController(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Get("/{coordinate}")
    public ArtifactTree get(String coordinate) {
        Gav gav = Gav.fromCoordinate(coordinate);
        return artifactRepository.find(gav)
                .orElseThrow(() -> new ResourceNotFoundException(gav));

    }
}
