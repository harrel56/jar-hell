package dev.harrel.jarhell;

import dev.harrel.jarhell.analyze.ArtifactNotFoundException;
import dev.harrel.jarhell.analyze.FilesInfo;
import dev.harrel.jarhell.analyze.MavenIndexService;
import dev.harrel.jarhell.model.Gav;
import io.avaje.config.Config;
import org.eclipse.jetty.client.api.ContentResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.inject.Singleton;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Singleton
public class MavenApiClient {
    private static final String CONTENT_URL = Config.get("maven.repo-url");

    private final CustomHttpClient httpClient;

    MavenApiClient(CustomHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public boolean checkIfArtifactExists(Gav gav) {
        var res = fetchRaw(createFileUrl(gav, "pom"));
        return res.getStatus() == 200;
    }

    public FilesInfo fetchFilesInfo(Gav gav) {
        String groupPath = gav.groupId().replace('.', '/');
        String path = "%s/%s/%s/".formatted(groupPath, gav.artifactId(), gav.version());
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8);
        String url = CONTENT_URL + "/" + encodedPath;
        var res = fetchRaw(url);
        if (res.getStatus() >= 400) {
            throw new ArtifactNotFoundException("HTTP call failed [%s] for url [%s]".formatted(res.getStatus(), url));
        }

        String filePrefix = "%s-%s".formatted(gav.artifactId(), gav.version());
        Document doc = Jsoup.parse(res.getContentAsString());
        List<String> suffixes = doc.getElementsByTag("a").stream()
                .map(el -> el.attr("href"))
                // subdirectories are not files; hrefs may be absolute depending on the repository manager
                .filter(href -> !href.endsWith("/"))
                .map(href -> href.substring(href.lastIndexOf('/') + 1))
                .filter(fileName -> fileName.startsWith(filePrefix))
                .map(fileName -> fileName.substring(filePrefix.length()))
                .toList();

        Set<String> classifiers = suffixes.stream()
                .filter(f -> f.startsWith("-"))
                .map(f -> f.indexOf('.') < 0 ? f.substring(1) : f.substring(1, f.indexOf('.')))
                .collect(Collectors.toSet());
        return new FilesInfo(parseExtensions(gav, suffixes), classifiers);
    }

    private ContentResponse fetchRaw(String url) {
        try {
            return httpClient.sendGet(URI.create(url), 5L);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalArgumentException("HTTP fetch failed for url [%s]".formatted(url), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e);
        }
    }

    public static String createFileUrl(Gav gav, String fileExtension) {
        String groupPath = gav.groupId().replace('.', '/');
        StringJoiner joiner = new StringJoiner("-")
                .add(gav.artifactId())
                .add(gav.version());
        if (gav.classifier() != null && !gav.classifier().isEmpty()) {
            joiner.add(gav.classifier());
        }
        String fileName = "%s.%s".formatted(joiner, fileExtension);
        String resource = "%s/%s/%s/%s".formatted(groupPath, gav.artifactId(), gav.version(), fileName);
        String encodedResource = URLEncoder.encode(resource, StandardCharsets.UTF_8);
        return CONTENT_URL + "/" + encodedResource;
    }

    private static Set<String> parseExtensions(Gav gav, List<String> suffixes) {
        if (gav.classifier() != null) {
            String prefix = "-%s.".formatted(gav.classifier());
            suffixes = suffixes.stream()
                    .filter(s -> s.startsWith(prefix))
                    .map(s -> s.substring(prefix.length() - 1))
                    .toList();
        } else {
            suffixes = suffixes.stream()
                    .filter(s -> s.startsWith("."))
                    .toList();
        }
        Set<String> res = new HashSet<>();
        for (String suffix : suffixes) {
            String last = suffix.substring(suffix.lastIndexOf('.') + 1);
            if (MavenIndexService.CHECKSUM_EXTENSIONS.contains(last)) {
                res.add(last);
            } else {
                res.add(suffix.substring(1));
            }
        }
        return res;
    }
}
