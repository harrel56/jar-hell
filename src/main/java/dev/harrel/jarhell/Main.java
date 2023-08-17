package dev.harrel.jarhell;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import org.neo4j.driver.*;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

public class Main {

    public static void main(String[] arg) {
        Processor processor = new Processor();

        Consumer<JavalinConfig> configConsumer = config -> {
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)));
        };

        Driver driver = createNeo4jDriver();
        Javalin server = Javalin.create(configConsumer)
                .attribute("neo4jDriver", driver)
                .get("/analyze", new AnalyzeHandler(processor))
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
