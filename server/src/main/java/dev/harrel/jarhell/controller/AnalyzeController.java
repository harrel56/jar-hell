package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.model.Gav;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

@Controller
class AnalyzeController {
    private final AnalyzeEngine analyzeEngine;

    AnalyzeController(AnalyzeEngine analyzeEngine) {
        this.analyzeEngine = analyzeEngine;
    }

    @Post("/api/v1/analyze")
    void analyze(Gav gav, Context ctx) {
        analyzeEngine.analyze(gav);
        ctx.status(HttpStatus.ACCEPTED);
    }

    @Post("/api/v1/analyze-and-wait")
    void analyzeAndWait(Gav gav, Context ctx) {
        analyzeEngine.analyze(gav).join();
        ctx.redirect("/api/v1/packages/%s?depth=1".formatted(gav));
    }
}
