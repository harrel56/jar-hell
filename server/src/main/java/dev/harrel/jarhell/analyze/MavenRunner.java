package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.maven.CustomDescriptorReaderDelegate;
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
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import javax.inject.Singleton;
import java.util.List;
import java.util.Set;

@Singleton
class MavenRunner {
    private static final Set<String> SCOPE_FILTER = Set.of("compile", "runtime");

    private static final String MAVEN_CENTRAL = Config.get("maven.repo-url");

    private final RepositorySystem repoSystem;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> remoteRepos;

    MavenRunner(RepositorySystem repoSystem, DefaultRepositorySystemSession session) {
        this.repoSystem = repoSystem;
        this.session = session;
        this.remoteRepos = List.of(new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL).build());
    }

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
            List<FlatDependency> deps = result.getDependencies().stream()
                    .map(MavenRunner::toFlatDependency)
                    .filter(dep -> SCOPE_FILTER.contains(dep.scope()))
                    .toList();

            // todo: url seems to be resolved incorrectly sometimes :(
            return new DescriptorInfo(model.getPackaging(), model.getName(), model.getDescription(),
                    model.getUrl(), model.getInceptionYear(), licenses, deps);
        } catch (ArtifactDescriptorException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static FlatDependency toFlatDependency(Dependency dep) {
        Artifact artifact = dep.getArtifact();
        Gav gav = new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
        String scope = dep.getScope().isBlank() ? "compile" : dep.getScope();
        return new FlatDependency(gav, dep.isOptional(), scope);
    }
}
