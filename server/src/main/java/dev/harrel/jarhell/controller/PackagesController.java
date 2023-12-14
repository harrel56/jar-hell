package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.QueryParam;

import java.util.Optional;

@Controller("/api/v1/packages")
class PackagesController {
    private final ArtifactRepository artifactRepository;

    PackagesController(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Get("/{coordinate}")
    ArtifactTree get(String coordinate, @QueryParam Integer depth) {
        Gav gav = Gav.fromCoordinate(coordinate)
                .orElseThrow(() -> new BadRequestException("Invalid artifact coordinate format [%s]".formatted(coordinate)));
        Integer depthParam = Optional.ofNullable(depth).orElse(-1);
        return artifactRepository.find(gav, depthParam)
                .orElseThrow(() -> new ResourceNotFoundException(gav));

    }
}
