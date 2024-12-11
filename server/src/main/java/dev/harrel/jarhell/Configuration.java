package dev.harrel.jarhell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.avaje.config.Config;
import io.avaje.http.api.AvajeJavalinPlugin;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.http.HttpClientConnectionFactory;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.neo4j.driver.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Factory
public class Configuration {

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

    @Bean(initMethod = "start", destroyMethod = "stop")
    HttpClient httpClient() {
        ClientConnector connector = new ClientConnector();
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;
        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(new HTTP2Client(connector));
        HttpClientTransportDynamic transport = new HttpClientTransportDynamic(connector, http1, http2);
        return new HttpClient(transport);
    }

    @Bean
    Consumer<JavalinConfig> javalinConfig(ObjectMapper objectMapper, List<AvajeJavalinPlugin> avajePlugins) {
        return config -> {
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
        };
    }
}
