package dev.harrel.jarhell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.avaje.config.Config;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import org.neo4j.driver.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.function.Consumer;

@Factory
class Configuration {

    @Bean
    ObjectMapper objectMapper() {
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

    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    @Bean
    Consumer<JavalinConfig> javalinConfig(ObjectMapper objectMapper) {
        return config -> {
            config.jsonMapper(new JavalinJackson(objectMapper));
            config.spaRoot.addFile("/", "/web/index.html");
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "/web/assets";
                staticFiles.hostedPath = "/assets";
                staticFiles.precompress = true;
                staticFiles.headers = Map.of("Cache-Control", "max-age=86400");
            });
            config.plugins.enableCors(cors ->
                    cors.add(it -> it.allowHost(
                            "http://localhost:5173",
                            "http://localhost:8060",
                            "https://jarhell.harrel.dev:8060"))
            );
        };
    }
}
