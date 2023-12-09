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

    protected static Driver driver;

    @BeforeAll
    protected static void beforeAll() {
        driver = GraphDatabase.driver(
                neo4jContainer.getBoltUrl(),
                AuthTokens.none(),
                Config.builder().withLogging(Logging.slf4j()).build()
        );
    }

    protected void clearDatabase() {
        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run("MATCH (n) DETACH DELETE n"));
            session.executeWriteWithoutResult(tx -> tx.run("CALL apoc.schema.assert({},{},true) YIELD label, key RETURN *"));
        }
    }
}
