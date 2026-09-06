package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.QueryParam;

import java.util.List;
import java.util.Optional;

@Controller("/api/v1/packages")
class PackagesController {
    private final ArtifactRepository artifactRepository;

    PackagesController(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Get("/{coordinate}/versions")
    List<String> getAllVersions(String coordinate, @QueryParam String classifier) {
        String[] parts = coordinate.split(":");
        if (parts.length != 2) {
            throw new BadRequestException("Invalid g:a format " + coordinate);
        }
        return artifactRepository.findAllVersions(parts[0], parts[1], classifier);
    }

    @Get("/search")
    List<SearchResult> search(@QueryParam String query) {
        if (query == null) {
            throw new BadRequestException("query parameter is required");
        }
        if (query.isEmpty()) {
            return List.of();
        }

        String[] split = query.split(":");
        List<Gav> gavs;
        if (split.length == 1) {
            gavs = artifactRepository.search(split[0].trim());
        } else {
            gavs = artifactRepository.search(split[0].trim(), split[1].trim());
        }
        return gavs.stream()
                .map(gav -> new SearchResult(gav.groupId(), gav.artifactId()))
                .toList();
    }

    @Get("/latest")
    List<ArtifactInfo> getLatest() {
        return artifactRepository.getLatest();
    }

    @Get("/count")
    int getAnalyzedCount() {
        return artifactRepository.getAnalyzedCount();
    }

    @Get("/{coordinate}")
    ArtifactTree get(String coordinate, @QueryParam Integer depth) {
        Gav gav = Gav.fromCoordinate(coordinate)
                .orElseThrow(() -> new BadRequestException("Invalid artifact coordinate format [%s]".formatted(coordinate)));
        Integer depthParam = Optional.ofNullable(depth).orElse(-1);
        return artifactRepository.find(gav, depthParam)
                .orElseThrow(() -> new ResourceNotFoundException(gav));

    }

    record SearchResult(String g, String a) {}
}
