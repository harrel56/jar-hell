package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.error.BadRequestException;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.QueryParam;

import java.util.List;

@Controller("/api/v1/maven")
class MavenApiController {
    private final MavenApiClient mavenApiClient;

    MavenApiController(MavenApiClient mavenApiClient) {
        this.mavenApiClient = mavenApiClient;
    }

    @Get("/search")
    List<MavenApiClient.SolrArtifact> search(@QueryParam String query) {
        return mavenApiClient.queryMavenSolr(query);
    }

    @Get("/versions")
    List<String> versions(@QueryParam String groupId, @QueryParam String artifactId) {
        if (groupId == null) {
            throw new BadRequestException("groupId parameter is required");
        }
        if (artifactId == null) {
            throw new BadRequestException("artifactId parameter is required");
        }
        return mavenApiClient.fetchArtifactVersions(groupId, artifactId);
    }
}
