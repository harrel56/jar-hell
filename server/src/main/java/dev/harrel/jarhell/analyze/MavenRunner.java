package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.maven.CustomDescriptorReaderDelegate;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;
import dev.harrel.jarhell.model.descriptor.Licence;
import io.avaje.config.Config;
import org.apache.maven.model.Model;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;

@Singleton
class MavenRunner {
    private static final Logger logger = LoggerFactory.getLogger(MavenRunner.class);

    private static final String MAVEN_CENTRAL = Config.get("maven.repo-url");

    private final RepositorySystem repoSystem;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> remoteRepos;

    MavenRunner(RepositorySystem repoSystem, DefaultRepositorySystemSession session) {
        this.repoSystem = repoSystem;
        this.session = session;
        this.remoteRepos = List.of(new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL).build());
    }

    // todo: this resolves all deps when only first level of deps is needed
    public DependencyNode resolveDependencies(Gav gavWithClassifier) {
        Gav gav = gavWithClassifier.stripClassifier();
        CollectRequest request = createCollectRequest(gav);
        try {
            CollectResult collectResult = repoSystem.collectDependencies(session, request);
            if (!collectResult.getCycles().isEmpty()) {
                logger.warn("Cycles detected in artifact [{}] deps: {}", gavWithClassifier, collectResult.getCycles());
                if (gavWithClassifier.classifier() != null) {
                    List<DependencyNode> filteredChildren = collectResult.getRoot().getChildren().stream()
                            .filter(child ->
                                    !gav.equals(new Gav(child.getArtifact().getGroupId(), child.getArtifact().getArtifactId(), child.getArtifact().getVersion())))
                            .toList();
                    collectResult.getRoot().setChildren(filteredChildren);
                } else {
                    throw new IllegalArgumentException(
                            "Cycles detected in artifact [%s] deps: %s".formatted(gavWithClassifier, collectResult.getCycles()));
                }
            }
            if (!collectResult.getExceptions().isEmpty()) {
                throw new IllegalArgumentException("Errors found in: " + gav);
            }
            return collectResult.getRoot();
        } catch (DependencyCollectionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public DescriptorInfo resolveDescriptor(Gav gavWithClassifier) {
        Gav gav = gavWithClassifier.stripClassifier();
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(new DefaultArtifact(gav.toString()), remoteRepos, null);
        try {
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor(session, request);
            Model model = (Model) result.getProperties().get(CustomDescriptorReaderDelegate.MODEL_KEY);
            if (model == null) {
                throw new IllegalArgumentException("Descriptor was not parsed into a model: " + gav);
            }
            List<Licence> licenses = model.getLicenses().stream()
                    .map(license -> new Licence(license.getName(), license.getUrl()))
                    .toList();
            // todo: url seems to be resolved incorrectly sometimes :(
            return new DescriptorInfo(model.getPackaging(), model.getName(), model.getDescription(), model.getUrl(), model.getInceptionYear(), licenses);
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
}
