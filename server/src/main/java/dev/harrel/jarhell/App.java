package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import dev.harrel.jarhell.handler.AnalyzeHandler;
import dev.harrel.jarhell.handler.PackagesHandler;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.inject.BeanScope;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

public class App {

    public static void main(String[] arg) {
        Logger mainLogger = LoggerFactory.getLogger(App.class);
        BeanScope beanScope = BeanScope.builder()
                .shutdownHook(true)
                .build();

        Driver driver = beanScope.get(Driver.class);
        DatabaseInitializer.initialize(driver);
        ObjectMapper objectMapper = beanScope.get(ObjectMapper.class);
        ArtifactRepository artifactRepository = new ArtifactRepository(driver, objectMapper);
        AnalyzeEngine analyzeEngine = new AnalyzeEngine(objectMapper, artifactRepository);
        AnalyzeHandler analyzeHandler = new AnalyzeHandler(analyzeEngine);
        PackagesHandler packagesHandler = new PackagesHandler(artifactRepository);

        Consumer<JavalinConfig> configConsumer = config -> {
            config.jsonMapper(new JavalinJackson(objectMapper));
            config.spaRoot.addFile("/", "/web/index.html");
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "/web/assets";
                staticFiles.hostedPath = "/assets";
                staticFiles.precompress = true;
                staticFiles.headers = Map.of("Cache-Control", "max-age=86400");
            });
        };
        Javalin server = Javalin.create(configConsumer)
                .post("/api/v1/analyze", analyzeHandler)
                .get("/api/v1/packages/{coordinate}", packagesHandler)
                .exception(ResourceNotFoundException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.NOT_FOUND);
                })
                .exception(ValueInstantiationException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), ExceptionUtils.getRootCause(e).getMessage()));
                    ctx.status(HttpStatus.BAD_REQUEST);
                })
                .exception(Exception.class, (e, ctx) -> {
                    mainLogger.error("Unhandled exception occurred", e);
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                })
                .start(8060);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            driver.close();
            server.close();
        }));
    }

    private static Driver createNeo4jDriver() {
        Map<String, String> env = System.getenv();
        URI dbUri = URI.create(env.get("neo4j.uri"));
        AuthToken authToken = AuthTokens.basic(env.get("neo4j.username"), env.get("neo4j.password"));
        Config config = Config.builder().withLogging(Logging.slf4j()).build();
        return GraphDatabase.driver(dbUri, authToken, config);
    }
}
