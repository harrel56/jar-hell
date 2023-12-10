package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.model.Gav;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

@Controller("/api/v1/analyze")
class AnalyzeController {
    private final AnalyzeEngine analyzeEngine;

    AnalyzeController(AnalyzeEngine analyzeEngine) {
        this.analyzeEngine = analyzeEngine;
    }

    @Post
    void analyze(Gav gav, Context ctx) {
        analyzeEngine.analyze(gav);
        ctx.status(HttpStatus.ACCEPTED);
    }
}
