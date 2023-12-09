package dev.harrel.jarhell;

import org.junit.jupiter.api.BeforeAll;
import org.neo4j.driver.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Neo4jLabsPlugin;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class Neo4jTest {
    @Container
    private static final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.11"))
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(Neo4jTest.class)))
            .withLabsPlugins(Neo4jLabsPlugin.APOC)
            .withoutAuthentication();

    public static Driver driver;

    @BeforeAll
    static void beforeAll() {
        driver = GraphDatabase.driver(
                neo4jContainer.getBoltUrl(),
                AuthTokens.none(),
                Config.builder().withLogging(Logging.slf4j()).build()
        );
    }
}
