package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.util.ConcurrentUtil;
import io.avaje.config.Config;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.inject.Singleton;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.harrel.jarhell.MavenApiClient.HTML_VERSIONS_PATTERN;

@Singleton
public class RepoWalker {
    private final String repoUrl = Config.get("maven.repo-url");
    private final HttpClient httpClient;

    public RepoWalker(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void walk(Consumer<State> consumer) {
        walkInternal(consumer, List.of());
    }

    private void walkInternal(Consumer<State> consumer, List<String> pathSegments) {
        URI uri = segmentsToUri(pathSegments);
        ContentResponse res;
        try {
            res = httpClient.GET(uri);
        } catch (ExecutionException | TimeoutException e) {
            throw new CompletionException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
        if (res.getStatus() >= 400) {
            throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(res.getStatus(), uri));
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

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            if (!versions.isEmpty()) {
                scope.fork(Executors.callable(() -> consumer.accept(createState(pathSegments, versions))));
            }
            for (String path : paths) {
                scope.fork(Executors.callable(() -> walkInternal(consumer, concatList(pathSegments, path))));
            }
            ConcurrentUtil.joinScope(scope);
        }
    }

    private URI segmentsToUri(List<String> pathSegments) {
        String path = pathSegments.stream()
                .map(seg -> URLEncoder.encode(seg, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return URI.create(repoUrl + "/" + path);
    }

    private List<String> concatList(List<String> pathSegments, String path) {
        return Stream.concat(pathSegments.stream(), Stream.of(path)).toList();
    }

    private State createState(List<String> pathSegments, List<String> versions) {
        pathSegments = pathSegments.stream()
                .map(v -> v.substring(0, v.length() - 1))
                .toList();
        String groupId = String.join(".", pathSegments.subList(0, pathSegments.size() - 1));
        String artifactId = pathSegments.getLast();
        return new State(groupId, artifactId, versions);
    }

    public record State(String groupId, String artifactId, List<String> versions) {}
}
