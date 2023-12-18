package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.MavenApiClient;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.QueryParam;

import java.util.List;

@Controller("/api/v1/maven/search")
class SearchController {
    private final MavenApiClient mavenApiClient;

    SearchController(MavenApiClient mavenApiClient) {
        this.mavenApiClient = mavenApiClient;
    }

    @Get
    List<MavenApiClient.SolrArtifact> search(@QueryParam String query) {
        return mavenApiClient.queryMavenSolr(query);
    }
}
