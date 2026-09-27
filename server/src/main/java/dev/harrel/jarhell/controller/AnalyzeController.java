package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.model.Gav;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Post;
import io.avaje.jex.http.Context;

import java.util.List;

@Controller("/api/v1")
class AnalyzeController {
    private static final List<String> CRAWLERS = List.of("googlebot", "storebot", "googleother", "google-inspectiontool", "cloudvertexbot", "google-extended");

    private final AnalyzeEngine analyzeEngine;

    AnalyzeController(AnalyzeEngine analyzeEngine) {
        this.analyzeEngine = analyzeEngine;
    }

    @Post("/analyze")
    void analyze(Gav gav, Context ctx) {
        if (!isWebCrawler(ctx)) {
            analyzeEngine.analyze(gav);
        }
    }

    @Post("/analyze-and-wait")
    void analyzeAndWait(Gav gav, Context ctx) {
        if (!isWebCrawler(ctx)) {
            analyzeEngine.analyze(gav).join();
            ctx.redirect("/api/v1/packages/%s?depth=1".formatted(gav));
        }
    }

    private static boolean isWebCrawler(Context ctx) {
        if (ctx.userAgent() == null) {
            return false;
        }
        for (String crawler : CRAWLERS) {
            if (ctx.userAgent().toLowerCase().contains(crawler)) {
                return true;
            }
        }
        return false;
    }
}
