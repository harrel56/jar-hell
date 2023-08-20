package dev.harrel.jarhell;

import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;
import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.ArtifactDescriptorReaderDelegate;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.supplier.CustomDescriptorReaderDelegate;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DependencyResolver {
    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";
    private static final String LOCAL_REPO_PATH = "build/local-repo";

    private final RepositorySystem repoSystem;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> remoteRepos;

    public DependencyResolver() {
        this.repoSystem = new RepositorySystemSupplier().get();
        this.session = MavenRepositorySystemUtils.newSession();
        this.remoteRepos = List.of(new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL).build());
        session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, new LocalRepository(LOCAL_REPO_PATH)));
        session.setRepositoryListener(new LoggingRepositoryListener());
        session.setConfigProperty(ArtifactDescriptorReaderDelegate.class.getName(), new CustomDescriptorReaderDelegate());
        // for maven profiles activation which depend on jdk version - value doesn't really matter
        session.setSystemProperties(Map.of("java.version", "17"));
    }

    // todo: this resolves all deps when only first level of deps is needed
    public DependencyNode resolveDependencies(Gav gav) {
        CollectRequest request = createCollectRequest(gav);
        try {
            CollectResult collectResult = repoSystem.collectDependencies(session, request);
            return collectResult.getRoot();
        } catch (DependencyCollectionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public DescriptorInfo resolveDescriptor(Gav gav) {
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(new DefaultArtifact(gav.toString()), remoteRepos, null);
        try {
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor(session, request);
            Model projectModel = (Model) result.getProperties().get(CustomDescriptorReaderDelegate.MODEL_KEY);
            return new DescriptorInfo(projectModel.getPackaging());
        } catch (ArtifactDescriptorException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private CollectRequest createCollectRequest(Gav gav) {
        Artifact artifact = new DefaultArtifact(gav.toString());
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.setRepositories(remoteRepos);
        return collectRequest;
    }

    private static class LoggingRepositoryListener extends AbstractRepositoryListener {
        private static final Logger logger = LoggerFactory.getLogger(LoggingRepositoryListener.class);

        @Override
        public void artifactDescriptorInvalid(RepositoryEvent event) {
            logger.warn("Invalid artifact descriptor for {}: {}", event.getArtifact(), event.getException().getMessage());
        }

        @Override
        public void artifactDescriptorMissing(RepositoryEvent event) {
            logger.warn("Missing artifact descriptor for {}", event.getArtifact());

        }

        @Override
        public void artifactDownloaded(RepositoryEvent event) {
            logger.info("Downloaded artifact {}", event.getArtifact());
        }

        @Override
        public void metadataInvalid(RepositoryEvent event) {
            logger.warn("Invalid metadata {}", event.getMetadata());
        }
    }
}
