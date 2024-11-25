package dev.harrel.jarhell.extension;

import dev.harrel.jarhell.App;
import dev.harrel.jarhell.DatabaseInitializer;
import io.avaje.config.Config;
import org.junit.jupiter.api.extension.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Neo4jLabsPlugin;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store;

public class EnvironmentExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver, TestInstancePostProcessor {
    private static final Namespace STORE_NAMESPACE = Namespace.create(EnvironmentExtension.class);
    private static final Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LoggerFactory.getLogger(EnvironmentExtension.class));

    public static final int PORT = 8686;

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
        Driver driver = getFromBeanScope(ec, Driver.class).orElseThrow();
        clearDatabase(driver);
        DatabaseInitializer.initialize(driver);
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return getFromBeanScope(ec, pc.getParameter().getType()).isPresent();
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
        return getFromBeanScope(ec, pc.getParameter().getType()).orElseThrow();
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Host.class)) {
                field.setAccessible(true);
                field.set(testInstance, "http://localhost:" + PORT);
            }
        }
    }

    private Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(STORE_NAMESPACE);
    }

    private <T> Optional<T> getFromBeanScope(ExtensionContext ec, Class<T> clazz) {
        return getStore(ec).get("state", State.class).app().getBeanScope().getOptional(clazz);
    }

    private State createState() {
        Network sharedNetwork = Network.newNetwork();

        CompletableFuture<Neo4jContainer<?>> neo4jContainerFuture = CompletableFuture.supplyAsync(this::startNeo4j);
        CompletableFuture<GenericContainer<?>> reposiliteContainerFuture = CompletableFuture.supplyAsync(() -> startReposilite(sharedNetwork));
        CompletableFuture<GenericContainer<?>> nginxContainerFuture = CompletableFuture.supplyAsync(() -> startNginx(sharedNetwork));
        CompletableFuture<WireMockContainer> wireMockContainerFuture = CompletableFuture.supplyAsync(this::startWireMock);
        /* wait for all containers */
        CompletableFuture.allOf(neo4jContainerFuture, reposiliteContainerFuture, wireMockContainerFuture).join();

        App app = startApp();
        return new State(
                app,
                neo4jContainerFuture.join(),
                reposiliteContainerFuture.join(),
                nginxContainerFuture.join(),
                wireMockContainerFuture.join()
        );
    }

    private Neo4jContainer<?> startNeo4j() {
        Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.25"))
                .withLogConsumer(logConsumer)
                .withLabsPlugins(Neo4jLabsPlugin.APOC)
                .withRandomPassword();
        neo4jContainer.start();
        Config.setProperty("neo4j.uri", neo4jContainer.getBoltUrl());
        Config.setProperty("neo4j.username", "neo4j");
        Config.setProperty("neo4j.password", neo4jContainer.getAdminPassword());

        return neo4jContainer;
    }

    private GenericContainer<?> startReposilite(Network network) {
        GenericContainer<?> reposiliteContainer = new GenericContainer<>(DockerImageName.parse("dzikoysk/reposilite:3.5.0"));
        reposiliteContainer.withNetwork(network);
        reposiliteContainer.withNetworkAliases("reposilite");
        reposiliteContainer.withLogConsumer(logConsumer);
        reposiliteContainer.setPortBindings(List.of("8080:8080"));
        reposiliteContainer.withCopyToContainer(MountableFile.forClasspathResource("/reposilite/repositories/snapshots"), "/app/data/repositories/snapshots");
        reposiliteContainer.start();
        return reposiliteContainer;
    }

    private GenericContainer<?> startNginx(Network network) {
        GenericContainer<?> reposiliteContainer = new GenericContainer<>(DockerImageName.parse("nginx:1.27.2-alpine"));
        reposiliteContainer.withNetwork(network);
        reposiliteContainer.withLogConsumer(logConsumer);
        reposiliteContainer.setPortBindings(List.of("8181:8181"));
        reposiliteContainer.withCopyToContainer(MountableFile.forClasspathResource("/reposilite/repositories/snapshots"), "/app/data/repositories/snapshots");
        reposiliteContainer.withCopyToContainer(MountableFile.forClasspathResource("/nginx/nginx.conf"), "/etc/nginx/nginx.conf");
        reposiliteContainer.start();
        return reposiliteContainer;
    }

    private WireMockContainer startWireMock() {
        /* wiremock direct resource API seems to be broken */
        URL resource = getClass().getResource("/wiremock/solr.json");
        WireMockContainer wireMockContainer = new WireMockContainer(DockerImageName.parse("wiremock/wiremock:3.3.1"))
                .withLogConsumer(logConsumer)
                .withMappingFromResource("solr", resource);
        wireMockContainer.setPortBindings(List.of("8282:8080"));
        wireMockContainer.start();
        return wireMockContainer;
    }

    private App startApp() {
        App app = new App();
        app.start(8686);
        return app;
    }

    private record State(App app,
                         Neo4jContainer<?> neo4jContainer,
                         GenericContainer<?> reposiliteContainer,
                         GenericContainer<?> nginxContainer,
                         WireMockContainer wireMockContainer) implements Closeable {
        @Override
        public void close() {
            app.close();
            neo4jContainer.stop();
            reposiliteContainer.stop();
            nginxContainer.stop();
            wireMockContainer.stop();
        }
    }
}
