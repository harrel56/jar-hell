package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;

@Controller("/api/v1/packages")
class PackagesController {
    private final ArtifactRepository artifactRepository;

    PackagesController(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Get("/{coordinate}")
    ArtifactTree get(String coordinate) {
        Gav gav = Gav.fromCoordinate(coordinate)
                .orElseThrow(() -> new BadRequestException("Invalid artifact coordinate format [%s]".formatted(coordinate)));
        return artifactRepository.find(gav)
                .orElseThrow(() -> new ResourceNotFoundException(gav));

    }
}
