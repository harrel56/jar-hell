package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.model.Gav;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.QueryParam;

import java.net.http.HttpClient;
import java.util.List;

@Controller("/api/v1/search")
class SearchController {
    private final HttpClient httpClient;

    SearchController(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Get
    List<Gav> search(@QueryParam String query) {
        System.out.println(query);
        return List.of();
    }
}
