package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.util.ConcurrentUtil;
import io.avaje.inject.PreDestroy;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.harrel.jarhell.MavenApiClient.HTML_VERSIONS_PATTERN;

@Singleton
public class RepoWalker {
    private static final Logger logger = LoggerFactory.getLogger(RepoWalker.class);

    private final HttpClient httpClient;
    /* server.bolt.thread_pool_max_size has default of 400 */
    private static final int CONSUMER_POOL_SIZE = 128;
    private static final int HTTP_POOL_SIZE = 64 * Runtime.getRuntime().availableProcessors();
    private final ExecutorService consumerService = Executors.newFixedThreadPool(CONSUMER_POOL_SIZE, Thread.ofVirtual().factory());
    private final ExecutorService httpService = Executors.newFixedThreadPool(HTTP_POOL_SIZE, Thread.ofVirtual().factory());

    public RepoWalker(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @PreDestroy
    void destroy() throws InterruptedException {
        logger.info("Shutting down...");
        consumerService.shutdownNow();
        httpService.shutdownNow();
        if (!consumerService.awaitTermination(2L, TimeUnit.SECONDS)) {
            logger.warn("consumerService failed to shutdown gracefully");
        }
        if (!httpService.awaitTermination(2L, TimeUnit.SECONDS)) {
            logger.warn("httpService failed to shutdown gracefully");
        }
    }

    public CompletableFuture<Summary> walk(String repoUrl, Consumer<ArtifactData> consumer) {
        logger.info("Starting repo walking: url={}, vConsumerPoolSize={}, vHttpPoolSize={}", repoUrl, CONSUMER_POOL_SIZE, HTTP_POOL_SIZE);
        Instant startTime = Instant.now();
        SharedState sharedState = new SharedState(repoUrl, consumer);
        return walkInternal(sharedState, List.of())
                .thenApply(_ -> {
                    Summary summary = new Summary(
                            Duration.between(startTime, Instant.now()),
                            sharedState.requestsCount.get(),
                            sharedState.artifactsCount.get(),
                            sharedState.failedRequestsCount.get(),
                            sharedState.failedArtifactsCount().get()
                    );
                    logger.info("Repo walking completed: duration={}, requests={}, artifacts={}, failedRequests={}, failedArtifacts={}",
                            summary.duration(), summary.requestsCount(), summary.artifactsCount(),
                            summary.failedRequestsCount(), summary.failedArtifactsCount());
                    return summary;
                });
    }

    private CompletableFuture<?> walkInternal(SharedState state, List<String> pathSegments) {
        URI uri = segmentsToUri(state, pathSegments);
        if (state.requestsCount().incrementAndGet() % 1000 == 0) {
            logger.info("Walking in progress... {} - {}", state.requestsCount(), uri);
        }
        ContentResponse res;
        try {
            res = httpClient.GET(uri);
        } catch (ExecutionException | TimeoutException e) {
            logger.warn("HTTP call failed for url [{}]", uri, e);
            return failure(state.failedRequestsCount());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
        if (res.getStatus() >= 400) {
            logger.warn("HTTP call failed [{}] for url [{}]", res.getStatus(), uri);
            return failure(state.failedRequestsCount());
        }

        Document doc = Jsoup.parse(res.getContentAsString());
        List<String> dirs = doc.getElementsByTag("a").stream()
                .map(el -> el.attr("href"))
                .filter(href -> href.endsWith("/") && !href.equals("../"))
                .toList();
        Map<Boolean, List<String>> partitioned = dirs.stream()
                .collect(Collectors.partitioningBy(dir -> HTML_VERSIONS_PATTERN.matcher(dir).matches()));
        List<String> paths = partitioned.get(false);
        List<String> versions = partitioned.get(true).stream()
                .map(v -> v.substring(0, v.indexOf('/')))
                .map(ComparableVersion::new)
                .sorted()
                .map(ComparableVersion::toString)
                .toList();

        List<CompletableFuture<?>> futures = new ArrayList<>(paths.size() + 1);
        if (!versions.isEmpty() && pathSegments.size() >= 2) {
            ArtifactData artifactData = createArtifactData(pathSegments, versions);
            CompletableFuture<Void> artifactFuture = CompletableFuture.supplyAsync(() -> {
                state.consumer().accept(artifactData);
                return state.artifactsCount.incrementAndGet();
            }, consumerService).handle((_, ex) -> {
                if (ex != null) {
                    logger.warn("Artifact processing failed for [{}:{}]", artifactData.groupId, artifactData.artifactId, ex);
                    return failure(state.failedArtifactsCount());
                } else {
                    return CompletableFuture.<Void>completedFuture(null);
                }
            }).thenCompose(Function.identity());
            futures.add(artifactFuture);
        }
        for (String path : paths) {
            CompletableFuture<?> cf = CompletableFuture.supplyAsync(
                            () -> walkInternal(state, concatList(pathSegments, path)), httpService)
                    .thenCompose(Function.identity());
            futures.add(cf);
        }
        return ConcurrentUtil.allOfFailFast(futures);
    }

    private URI segmentsToUri(SharedState state, List<String> pathSegments) {
        String path = pathSegments.stream()
                .map(v -> v.substring(0, v.length() - 1))
                .map(seg -> URLEncoder.encode(seg, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return URI.create(state.repoUrl() + "/" + path + (path.isEmpty() ? "" : "/"));
    }

    private List<String> concatList(List<String> pathSegments, String path) {
        return Stream.concat(pathSegments.stream(), Stream.of(path)).toList();
    }

    private ArtifactData createArtifactData(List<String> pathSegments, List<String> versions) {
        pathSegments = pathSegments.stream()
                .map(v -> v.substring(0, v.length() - 1))
                .toList();
        String groupId = String.join(".", pathSegments.subList(0, pathSegments.size() - 1));
        String artifactId = pathSegments.getLast();
        return new ArtifactData(groupId, artifactId, versions);
    }

    private static CompletableFuture<Void> failure(AtomicLong counter) {
        counter.incrementAndGet();
        return CompletableFuture.completedFuture(null);
    }

    public record ArtifactData(String groupId, String artifactId, List<String> versions) {}

    public record Summary(Duration duration,
                          long requestsCount,
                          long artifactsCount,
                          long failedRequestsCount,
                          long failedArtifactsCount) {}

    private record SharedState(String repoUrl,
                               Consumer<ArtifactData> consumer,
                               AtomicLong requestsCount,
                               AtomicLong artifactsCount,
                               AtomicLong failedRequestsCount,
                               AtomicLong failedArtifactsCount) {
        private SharedState(String repoUrl, Consumer<ArtifactData> consumer) {
            this(repoUrl, consumer, new AtomicLong(), new AtomicLong(), new AtomicLong(), new AtomicLong());
        }
    }
}
