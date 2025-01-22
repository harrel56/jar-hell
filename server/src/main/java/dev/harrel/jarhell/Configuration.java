package dev.harrel.jarhell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.analyze.ArtifactProcessor;
import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.config.Config;
import io.avaje.http.api.AvajeJavalinPlugin;
import io.avaje.http.api.InvalidTypeArgumentException;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.http.HttpClientConnectionFactory;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Factory
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    @Bean
    public ScheduledFuture<?> taskCountLogger(AnalyzeEngine engine) {
        return Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() ->
                        logger.info("Task count = {}, max = {}", engine.taskCount.get(), engine.maxTaskCount.get()),
                0, 1, TimeUnit.SECONDS);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new JavaTimeModule());
    }

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
    Javalin javalinServer(ObjectMapper objectMapper, List<AvajeJavalinPlugin> avajePlugins) {
        final String apiToken = Config.get("API_TOKEN");
        return Javalin.create(config -> {
                    config.jsonMapper(new JavalinJackson(objectMapper, true));
                    config.spaRoot.addFile("/", "/web/index.html");
                    config.staticFiles.add(staticFiles -> {
                        staticFiles.directory = "/web/assets";
                        staticFiles.hostedPath = "/assets";
                        staticFiles.precompress = true;
                        staticFiles.headers = Map.of("Cache-Control", "max-age=86400");
                    });
                    if (Config.enabled("jar-hell.dev-mode", false)) {
                        config.bundledPlugins.enableCors(cors ->
                                cors.addRule(CorsPluginConfig.CorsRule::anyHost)
                        );
                    }
                    avajePlugins.forEach(config::registerPlugin);
                })
                .beforeMatched("/technical/*", ctx -> {
                    String token = Optional.ofNullable(ctx.header("Authorization"))
                            .map(header -> header.split(" "))
                            .filter(values -> values.length == 2 && "Bearer".equals(values[0]))
                            .map(values -> values[1])
                            .orElse(null);
                    if (!apiToken.equals(token)) {
                        Thread.sleep(2000);
                        throw new UnauthorizedResponse("Invalid token");
                    }
                })
                .exception(UnauthorizedResponse.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.UNAUTHORIZED);
                })
                .exception(ResourceNotFoundException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.NOT_FOUND);
                })
                .exception(ValueInstantiationException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), ExceptionUtils.getRootCause(e).getMessage()));
                    ctx.status(HttpStatus.BAD_REQUEST);
                })
                .exception(BadRequestException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.BAD_REQUEST);
                })
                .exception(InvalidTypeArgumentException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.BAD_REQUEST);
                })
                .exception(Exception.class, (e, ctx) -> {
                    logger.error("Unhandled exception occurred", e);
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }
}
