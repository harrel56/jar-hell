package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.analyze.RepoWalker;
import dev.harrel.jarhell.model.Gav;
import io.avaje.config.Config;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

@Controller("/api/v1")
class AnalyzeController {
    private final AnalyzeEngine analyzeEngine;
    private final RepoWalker repoWalker;

    AnalyzeController(AnalyzeEngine analyzeEngine, RepoWalker repoWalker) {
        this.analyzeEngine = analyzeEngine;
        this.repoWalker = repoWalker;
    }

    @Post("/analyze")
    void analyze(Gav gav, Context ctx) {
        analyzeEngine.analyze(gav);
        ctx.status(HttpStatus.ACCEPTED);
    }

    @Post("/analyze-and-wait")
    void analyzeAndWait(Gav gav, Context ctx) {
        analyzeEngine.analyze(gav).join();
        ctx.redirect("/api/v1/packages/%s?depth=1".formatted(gav));
    }

    @Post("/crawl")
    void crawl(Context ctx) {
        repoWalker.walk(Config.get("maven.repo-url"), data -> analyzeEngine.analyze(new Gav(data.groupId(), data.artifactId(), data.versions().getLast())).join());
        ctx.status(HttpStatus.ACCEPTED);
    }
}
