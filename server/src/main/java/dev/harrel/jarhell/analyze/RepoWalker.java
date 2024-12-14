package dev.harrel.jarhell.analyze;

import io.avaje.config.Config;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static dev.harrel.jarhell.MavenApiClient.HTML_VERSIONS_PATTERN;

public class RepoWalker {
    private final String repoUrl = Config.get("maven.repo-url");
    private final HttpClient httpClient;

    public RepoWalker(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /// - fetch page
    /// - get all catalogs, split to version and non-version
    /// - (subtask) for version list: get it with current path (extract GA) and analyze
    /// - (subtask) for non-version: tart over with new URI
    public void walk(Consumer<State> consumer) {
        try {
            walkInternal(consumer, List.of());
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalArgumentException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    private void walkInternal(Consumer<State> consumer, List<String> pathSegments) throws ExecutionException, InterruptedException, TimeoutException {
        URI uri = segmentsToUri(pathSegments);
        ContentResponse res = httpClient.GET(uri);
        if (res.getStatus() >= 400) {
            throw new IllegalArgumentException("HTTP call failed [%s] for url [%s]".formatted(res.getStatus(), uri));
        }

        Document doc = Jsoup.parse(res.getContentAsString());
        List<String> dirs = doc.getElementsByTag("a").stream()
                .map(el -> el.attr("href"))
                .filter(href -> href.endsWith("/") && !href.equals("../"))
                .map(href -> href.substring(0, href.indexOf('/')))
                .toList();
        Map<Boolean, List<String>> partitioned = dirs.stream()
                .collect(Collectors.partitioningBy(dir -> HTML_VERSIONS_PATTERN.matcher(dir).matches()));
        List<String> paths = partitioned.get(false);
        List<String> versions = partitioned.get(true).stream()
                .map(ComparableVersion::new)
                .sorted()
                .map(ComparableVersion::toString)
                .toList();

        // todo: use structured concurrency and visit all paths
        consumer.accept(createState(pathSegments, versions));

    }

    private URI segmentsToUri(List<String> pathSegments) {
        String path = pathSegments.stream()
                .map(seg -> URLEncoder.encode(seg, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return URI.create(repoUrl + "/" + path);
    }

    private State createState(List<String> pathSegments, List<String> versions) {
        String groupId = String.join(".", pathSegments.subList(0, pathSegments.size() - 1));
        String artifactId = pathSegments.getLast();
        return new State(groupId, artifactId, versions);
    }

    public record State(String groupId, String artifactId, List<String> versions) {}
}
