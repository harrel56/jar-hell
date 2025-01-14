package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.analyze.ArtifactProcessor;
import dev.harrel.jarhell.analyze.RepoWalker;
import dev.harrel.jarhell.model.Gav;
import io.avaje.config.Config;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.avaje.http.api.QueryParam;

@Controller("/technical/v1")
class TechnicalController {
    private final AnalyzeEngine analyzeEngine;
    private final RepoWalker repoWalker;
    private final ArtifactProcessor artifactProcessor;

    TechnicalController(AnalyzeEngine analyzeEngine, RepoWalker repoWalker, ArtifactProcessor artifactProcessor) {
        this.analyzeEngine = analyzeEngine;
        this.repoWalker = repoWalker;
        this.artifactProcessor = artifactProcessor;
    }

    @Post("/crawl")
    RepoWalker.Summary crawl() {
        return repoWalker.walk(
                Config.get("maven.repo-url"),
                data -> analyzeEngine.saveUnresolved(new Gav(data.groupId(), data.artifactId(), data.versions().getLast()))
        ).join();
    }

    @Post("/processor/start")
    void startProcessor(@QueryParam int concurrency) {
        artifactProcessor.start(concurrency);
    }

    @Post("/processor/stop")
    void stopProcessor() {
        artifactProcessor.stop();
    }
}
