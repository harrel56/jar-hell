package dev.harrel.jarhell;

import io.avaje.config.Config;
import org.junit.jupiter.api.extension.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Neo4jLabsPlugin;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.Closeable;
import java.util.List;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store;

public class EnvironmentExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {
    private static final Namespace STORE_NAMESPACE = Namespace.create(EnvironmentExtension.class);

    public static void clearDatabase(Driver driver) {
        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run("MATCH (n) DETACH DELETE n"));
            session.executeWriteWithoutResult(tx -> tx.run("CALL apoc.schema.assert({},{},true) YIELD label, key RETURN *"));
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        State state = getStore(context).getOrComputeIfAbsent("state", k -> createState(), State.class);
        System.out.println("state = " + state);
    }

    @Override
    public void beforeEach(ExtensionContext ec) {
        Driver driver = getDriver(ec);
        clearDatabase(driver);
        DatabaseInitializer.initialize(driver);
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return Driver.class.isAssignableFrom(pc.getParameter().getType());
    }

    @Override
    public Driver resolveParameter(ParameterContext pc, ExtensionContext ec) {
        return getDriver(ec);
    }

    private Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(STORE_NAMESPACE);
    }

    private Driver getDriver(ExtensionContext ec) {
        return getStore(ec).get("state", State.class).app().getBeanScope().get(Driver.class);
    }

    private State createState() {
        Neo4jContainer<?> neo4jContainer = startNeo4jContainer();
        GenericContainer<?> reposiliteContainer = startReposilite();

        App app = startApp();
        return new State(app, neo4jContainer, reposiliteContainer);
    }

    private Neo4jContainer<?> startNeo4jContainer() {
        Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.11"))
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EnvironmentExtension.class)))
                .withLabsPlugins(Neo4jLabsPlugin.APOC)
                .withRandomPassword();
        neo4jContainer.start();
        Config.setProperty("neo4j.uri", neo4jContainer.getBoltUrl());
        Config.setProperty("neo4j.username", "neo4j");
        Config.setProperty("neo4j.password", neo4jContainer.getAdminPassword());

        return neo4jContainer;
    }

    private GenericContainer<?> startReposilite() {
        GenericContainer<?> reposiliteContainer = new GenericContainer<>(DockerImageName.parse("dzikoysk/reposilite:3.5.0"));
        reposiliteContainer.setPortBindings(List.of("8181:8080"));
        reposiliteContainer.withCopyToContainer(MountableFile.forClasspathResource("/reposilite"), "/app/data");
        reposiliteContainer.start();
        return reposiliteContainer;
    }

    private App startApp() {
        App app = new App();
        app.start();
        return app;
    }

    private record State(App app,
                         Neo4jContainer<?> neo4jContainer,
                         GenericContainer<?> reposiliteContainer) implements Closeable {
        @Override
        public void close() {
            app.close();
            neo4jContainer.stop();
            reposiliteContainer.stop();
        }
    }
}
