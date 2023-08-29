package dev.harrel.jarhell.handler;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AnalyzeHandler implements Handler {
    private final AnalyzeEngine analyzeEngine;

    public AnalyzeHandler(AnalyzeEngine analyzeEngine) {
        this.analyzeEngine = analyzeEngine;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        Gav gav = ctx.bodyAsClass(Gav.class);
        CompletableFuture<ArtifactTree> artifactTree = analyzeEngine.analyze(gav);

        boolean waitForResponse = ctx.queryParam("blocking") != null;
        if (waitForResponse) {
            ctx.json(artifactTree.join());
            ctx.status(HttpStatus.OK);
        } else {
            ctx.status(HttpStatus.ACCEPTED);
        }
    }
}
