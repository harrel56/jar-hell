package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.ArtifactProcessor;
import dev.harrel.jarhell.analyze.MavenIndexService;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.avaje.http.api.QueryParam;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

@Controller("/technical/v1")
class TechnicalController {
    private final MavenIndexService indexService;
    private final ArtifactProcessor artifactProcessor;

    TechnicalController(MavenIndexService indexService, ArtifactProcessor artifactProcessor) {
        this.indexService = indexService;
        this.artifactProcessor = artifactProcessor;
    }

    @Post("/refresh-index")
    void refreshIndex(Context ctx) {
        ctx.status(HttpStatus.ACCEPTED);
        Thread.ofVirtual().start(indexService::scanIndex);
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
