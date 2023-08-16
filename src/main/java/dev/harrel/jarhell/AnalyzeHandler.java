package dev.harrel.jarhell;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AnalyzeHandler implements Handler {
    private final Processor processor;

    public AnalyzeHandler(Processor processor) {
        this.processor = processor;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String groupId = Optional.ofNullable(ctx.queryParam("groupId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'groupId' is required"));
        String artifactId = Optional.ofNullable(ctx.queryParam("artifactId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'artifactId' is required"));
        ArtifactInfo artifactInfo = processor.process(groupId, artifactId);

        ctx.json(artifactInfo);
    }
}
