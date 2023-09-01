package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.model.Gav;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

@Controller("/api/v1/analyze")
public class AnalyzeController {
    private final AnalyzeEngine analyzeEngine;

    public AnalyzeController(AnalyzeEngine analyzeEngine) {
        this.analyzeEngine = analyzeEngine;
    }

    @Post
    public void analyze2(Gav gav, Context ctx) {
        analyzeEngine.analyze(gav);
        ctx.status(HttpStatus.ACCEPTED);
    }
}
