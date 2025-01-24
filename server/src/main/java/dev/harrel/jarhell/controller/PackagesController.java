package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.error.ResourceNotFoundException;
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

    @Get
    List<ArtifactTree> getAllVersions(@QueryParam String groupId,
                                      @QueryParam String artifactId,
                                      @QueryParam String classifier) {
        if (groupId == null) {
            throw new BadRequestException("groupId parameter is required");
        }
        if (artifactId == null) {
            throw new BadRequestException("artifactId parameter is required");
        }
        return artifactRepository.findAllVersions(groupId, artifactId, classifier);
    }

    @Get("/search")
    List<String> search(@QueryParam String query) {
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
                .map(gav -> "%s:%s".formatted(gav.groupId(), gav.artifactId()))
                .toList();
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
