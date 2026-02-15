package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.analyze.ArtifactProcessor;
import dev.harrel.jarhell.analyze.MavenIndexService;
import dev.harrel.jarhell.analyze.RepoWalker;
import dev.harrel.jarhell.model.Gav;
import io.avaje.config.Config;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.avaje.http.api.QueryParam;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

@Controller("/technical/v1")
class TechnicalController {
    private final MavenIndexService indexService;
    private final AnalyzeEngine analyzeEngine;
    private final RepoWalker repoWalker;
    private final ArtifactProcessor artifactProcessor;

    TechnicalController(MavenIndexService indexService, AnalyzeEngine analyzeEngine, RepoWalker repoWalker, ArtifactProcessor artifactProcessor) {
        this.indexService = indexService;
        this.analyzeEngine = analyzeEngine;
        this.repoWalker = repoWalker;
        this.artifactProcessor = artifactProcessor;
    }

    @Post("/refresh-index")
    void refreshIndex(Context ctx) {
        ctx.status(HttpStatus.ACCEPTED);
        Thread.ofVirtual().start(indexService::scanIndex);
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
