package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.maven.CustomDescriptorReaderDelegate;
import dev.harrel.jarhell.model.CollectedDependencies;
import dev.harrel.jarhell.model.FlatDependency;
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
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import javax.inject.Singleton;
import java.util.List;

@Singleton
class MavenRunner {
    private static final String MAVEN_CENTRAL = Config.get("maven.repo-url");

    private final RepositorySystem repoSystem;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> remoteRepos;

    MavenRunner(RepositorySystem repoSystem, DefaultRepositorySystemSession session) {
        this.repoSystem = repoSystem;
        this.session = session;
        this.remoteRepos = List.of(new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL).build());
    }

    public CollectedDependencies collectDependencies(Gav gavWithClassifier) {
        Gav gav = gavWithClassifier.stripClassifier();
        CollectRequest request = createCollectRequest(gav);
        try {
            CollectResult collectResult = repoSystem.collectDependencies(session, request);
            if (!collectResult.getExceptions().isEmpty()) {
                throw new IllegalArgumentException("Errors found in %s: %s".formatted(gav, collectResult.getExceptions()));
            }

            List<FlatDependency> directDependencies = collectResult.getRoot().getChildren().stream()
                    .map(DependencyNode::getDependency)
                    .map(MavenRunner::toFlatDependency)
                    .toList();
            PreorderNodeListGenerator visitor = new PreorderNodeListGenerator();
            collectResult.getRoot().accept(visitor);
            List<FlatDependency> allDependencies = visitor.getDependencies(true).stream()
                    .map(MavenRunner::toFlatDependency)
                    .filter(dep -> !dep.gav().equals(gav))
                    .toList();

            return new CollectedDependencies(directDependencies, allDependencies);
        } catch (DependencyCollectionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // todo: this should just use ModelBuilder, then we can remove CustomDescriptorReaderDelegate
    public DescriptorInfo resolveDescriptor(Gav gavWithClassifier) {
        Gav gav = gavWithClassifier.stripClassifier();
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(new DefaultArtifact(gav.toString()), remoteRepos, null);
        try {
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor(session, request);
            Model model = (Model) result.getProperties().get(CustomDescriptorReaderDelegate.MODEL_KEY);
            if (model == null) {
                throw new IllegalArgumentException("Descriptor was not parsed into a model (couldn't retrieve pom?): " + gav);
            }
            List<Licence> licenses = model.getLicenses().stream()
                    .map(license -> new Licence(license.getName(), license.getUrl()))
                    .toList();

            // todo: url seems to be resolved incorrectly sometimes :(
            return new DescriptorInfo(model.getPackaging(), model.getName(), model.getDescription(),
                    model.getUrl(), model.getInceptionYear(), licenses);
        } catch (ArtifactDescriptorException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private CollectRequest createCollectRequest(Gav gav) {
        Artifact artifact = new DefaultArtifact(gav.groupId(), gav.artifactId(), gav.classifier(), "jar", gav.version());
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.setRepositories(remoteRepos);
        return collectRequest;
    }

    private static Gav toGav(Dependency dep) {
        Artifact artifact = dep.getArtifact();
        return new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
    }

    private static FlatDependency toFlatDependency(Dependency dep) {
        String scope = dep.getScope().isBlank() ? "compile" : dep.getScope();
        return new FlatDependency(toGav(dep), dep.isOptional(), scope);
    }
}
