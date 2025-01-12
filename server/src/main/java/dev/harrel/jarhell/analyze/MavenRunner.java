package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.maven.CustomDescriptorReaderDelegate;
import dev.harrel.jarhell.model.CollectedDependencies;
import dev.harrel.jarhell.model.FlatDependency;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.LicenseType;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;
import dev.harrel.jarhell.model.descriptor.License;
import io.avaje.config.Config;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.MetadataTransferException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Singleton
class MavenRunner {
    private static final String MAVEN_CENTRAL = Config.get("maven.repo-url");
    private static final Logger logger = LoggerFactory.getLogger(MavenRunner.class);

    private final RepositorySystem repoSystem;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepos;

    MavenRunner(RepositorySystem repoSystem, RepositorySystemSession session) {
        this.repoSystem = repoSystem;
        this.session = session;
        this.remoteRepos = List.of(new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL).build());
    }

    public CollectedDependencies collectDependencies(Gav gav) {
        CollectRequest request = createCollectRequest(gav);
        CollectResult collectResult;
        try {
            collectResult = repoSystem.collectDependencies(session, request);
        } catch (DependencyCollectionException e) {
            logger.warn("Dependency collection failed", e);
            collectResult = e.getResult();
        }

        List<FlatDependency> directDependencies = collectResult.getRoot().getChildren().stream()
                .map(DependencyNode::getDependency)
                .map(MavenRunner::toFlatDependency)
                .toList();

        PreorderNodeListGenerator visitor = new PreorderNodeListGenerator();
        collectResult.getRoot().accept(visitor);
        Stream<FlatDependency> allDependenciesStream = visitor.getDependencies(true).stream()
                .map(MavenRunner::toFlatDependency)
                .filter(dep -> !dep.gav().equals(gav));
        Stream<FlatDependency> failedDepsStream = getFailureCauses(collectResult.getExceptions());
        List<FlatDependency> allDependencies = Stream.concat(allDependenciesStream, failedDepsStream).toList();

        return new CollectedDependencies(directDependencies, allDependencies);
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
            String scmUrl = Optional.ofNullable(model.getScm())
                    .map(Scm::getUrl)
                    .orElse(null);
            String issuesUrl = Optional.ofNullable(model.getIssueManagement())
                    .map(IssueManagement::getUrl)
                    .orElse(null);
            List<License> licenses = model.getLicenses().stream()
                    .map(license -> new License(license.getName(), license.getUrl()))
                    .toList();
            List<LicenseType> licenseTypes = licenses.stream()
                    .map(LicenseType::categorize)
                    .toList();

            // todo: url seems to be resolved incorrectly sometimes :(
            return new DescriptorInfo(model.getPackaging(), model.getName(), model.getDescription(),
                    model.getUrl(), scmUrl, issuesUrl, model.getInceptionYear(), licenses, licenseTypes);
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

    private static Gav toGav(Artifact artifact) {
        return new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
    }

    private static Gav toGav(Dependency dep) {
        return toGav(dep.getArtifact());
    }

    private static FlatDependency toFlatDependency(Dependency dep) {
        String scope = dep.getScope().isBlank() ? "compile" : dep.getScope();
        return new FlatDependency(toGav(dep), dep.isOptional(), scope);
    }

    private static FlatDependency toFlatDependency(Artifact artifact) {
        return new FlatDependency(toGav(artifact), false, "compile");
    }

    private static FlatDependency toFlatDependency(Metadata metadata) {
        Gav gav = new Gav(metadata.getGroupId(), metadata.getArtifactId(), metadata.getVersion());
        return new FlatDependency(gav, false, "compile");
    }

    private static Stream<FlatDependency> getFailureCauses(List<Exception> exceptions) {
        return exceptions.stream()
                .<List<FlatDependency>>map(e ->
                        switch (e) {
                            case ArtifactDescriptorException ade ->
                                    List.of(toFlatDependency(ade.getResult().getArtifact()));
                            case ArtifactResolutionException are ->
                                    are.getResults().stream()
                                    .map(a -> toFlatDependency(a.getArtifact()))
                                    .toList();
                            case ArtifactTransferException ate ->
                                List.of(toFlatDependency(ate.getArtifact()));
                            case MetadataTransferException mte ->
                                List.of(toFlatDependency(mte.getMetadata()));
                            default -> List.of();
                        }
                ).flatMap(List::stream);
    }
}
