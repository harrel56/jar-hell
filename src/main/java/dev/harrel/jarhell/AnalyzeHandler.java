package dev.harrel.jarhell;

import dev.harrel.jarhell.model.ArtifactInfo;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.ConsoleRepositoryListener;
import org.eclipse.aether.supplier.ConsoleTransferListener;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AnalyzeHandler implements Handler {
    private final Processor processor;

    public AnalyzeHandler(Processor processor) {
        this.processor = processor;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String groupId = Optional.ofNullable(ctx.queryParam("groupId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'groupId' is required"));
        String artifactId = Optional.ofNullable(ctx.queryParam("artifactId"))
                .orElseThrow(() -> new IllegalArgumentException("Argument 'artifactId' is required"));
        ArtifactInfo artifactInfo = processor.process(groupId, artifactId);

        RepositorySystem system = new RepositorySystemSupplier().get();

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        Artifact artifact = new DefaultArtifact("%s:%s:%s".formatted(groupId, artifactId, artifactInfo.gav().version()));
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.setRepositories(newRepositories());
        CollectResult collectResult = system.collectDependencies(session, collectRequest);


        ctx.json(artifactInfo);
    }

    public static List<RemoteRepository> newRepositories() {
        return new ArrayList<>(Collections.singletonList(newCentralRepository()));
    }

    private static RemoteRepository newCentralRepository() {
        return new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
    }
}
