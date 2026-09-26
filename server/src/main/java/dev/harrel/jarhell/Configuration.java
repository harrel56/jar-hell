package dev.harrel.jarhell;

import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.analyze.ArtifactProcessor;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.config.Config;
import io.avaje.http.api.InvalidTypeArgumentException;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.jex.Jex;
import io.avaje.jex.Routing;
import io.avaje.jex.http.HttpResponseException;
import io.avaje.jex.http.HttpStatus;
import io.avaje.jex.staticcontent.StaticContent;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Factory
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    @Bean
    Driver neo4jDriver() {
        URI dbUri = Config.getURI("neo4j.uri");
        AuthToken authToken = AuthTokens.basic(Config.get("neo4j.username"), Config.get("neo4j.password"));
        var config = org.neo4j.driver.Config.builder().withLogging(Logging.slf4j()).build();
        Driver driver = GraphDatabase.driver(dbUri, authToken, config);
        DatabaseInitializer.initialize(driver);
        return driver;
    }

    @Bean(destroyMethod = "stop")
    CustomHttpClient httpClient() throws Exception {
        ClientConnector connector = new ClientConnector();
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;
        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(new HTTP2Client(connector));
        HttpClientTransportDynamic transport = new HttpClientTransportDynamic(connector, http1, http2);
        CustomHttpClient httpClient = new CustomHttpClient(transport);
        httpClient.setMaxRequestsQueuedPerDestination(Integer.MAX_VALUE);
        httpClient.start();
        return httpClient;
    }

    @Bean
    ArtifactProcessor artifactProcessor(ArtifactRepository repo, AnalyzeEngine engine) {
        return new ArtifactProcessor(repo, engine);
    }

    @Bean
    Jex javalinServer(List<Routing.HttpService> routes) {
        StaticContent webBundle =
                StaticContent.ofClassPath("/web")
                        .route("/*")
                        .spaRoot("index.html")
                        .putResponseHeader("Cache-Control", "max-age=86400")
                        .preCompress()
                        .build();

        final String apiToken = Config.get("API_TOKEN");
        return Jex.create()
                .plugin(webBundle)
                .routing(routes)
                .before(ctx -> {
                    if (ctx.path().startsWith("/technical/")) {
                        String token = Optional.ofNullable(ctx.header("Authorization"))
                                .map(header -> header.split(" "))
                                .filter(values -> values.length == 2 && "Bearer".equals(values[0]))
                                .map(values -> values[1])
                                .orElse(null);
                        if (!apiToken.equals(token)) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new HttpResponseException(HttpStatus.UNAUTHORIZED_401, "Invalid token");
                            }
                            throw new HttpResponseException(HttpStatus.UNAUTHORIZED_401, "Invalid token");
                        }
                    }
                })
                .error(InvalidTypeArgumentException.class, (ctx, e) -> {
                    ctx.status(HttpStatus.BAD_REQUEST_400);
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                })
                .error(HttpResponseException.class, (ctx, e) -> {
                    ctx.status(e.status());
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                })
                .error(Exception.class, (ctx, e) -> {
                    logger.error("Unhandled exception occurred", e);
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                });
    }
}
