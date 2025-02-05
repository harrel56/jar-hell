package dev.harrel.jarhell;

import dev.harrel.jarhell.maven.LoggingRepositoryListener;
import io.avaje.config.Config;
import io.avaje.inject.Bean;
import io.avaje.inject.Component;
import io.avaje.inject.Factory;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.*;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.*;
import org.eclipse.aether.internal.impl.*;
import org.eclipse.aether.internal.impl.checksum.*;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.collect.DependencyCollectorDelegate;
import org.eclipse.aether.internal.impl.collect.bf.BfDependencyCollector;
import org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector;
import org.eclipse.aether.internal.impl.filter.DefaultRemoteRepositoryFilterManager;
import org.eclipse.aether.internal.impl.filter.GroupIdRemoteRepositoryFilterSource;
import org.eclipse.aether.internal.impl.filter.PrefixesRemoteRepositoryFilterSource;
import org.eclipse.aether.internal.impl.resolution.TrustedChecksumsArtifactResolverPostProcessor;
import org.eclipse.aether.internal.impl.synccontext.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NameMapper;
import org.eclipse.aether.internal.impl.synccontext.named.NameMappers;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactoryImpl;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.named.providers.FileLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalReadWriteLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalSemaphoreNamedLockFactory;
import org.eclipse.aether.named.providers.NoopNamedLockFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.spi.checksums.ProvidedChecksumsSource;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import dev.harrel.jarhell.maven.CustomDescriptorReaderDelegate;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.ChecksumExtractor;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.transport.http.Nexus2ChecksumExtractor;
import org.eclipse.aether.transport.http.XChecksumChecksumExtractor;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component.Import({
        DefaultFileProcessor.class, DefaultTrackingFileManager.class,
        DefaultLocalPathComposer.class, DefaultLocalPathPrefixComposerFactory.class, DefaultRepositorySystemLifecycle.class,
        DefaultOfflineController.class, DefaultUpdatePolicyAnalyzer.class, DefaultChecksumPolicyProvider.class,
        DefaultUpdateCheckManager.class, NamedLockFactoryAdapterFactoryImpl.class, DefaultSyncContextFactory.class,
        DefaultRepositoryLayoutProvider.class, SimpleLocalRepositoryManagerFactory.class,
        EnhancedLocalRepositoryManagerFactory.class, DefaultLocalRepositoryProvider.class, DefaultRemoteRepositoryManager.class,
        DefaultRemoteRepositoryFilterManager.class, DefaultRepositoryEventDispatcher.class, DefaultTransporterProvider.class,
        BasicRepositoryConnectorFactory.class, DefaultRepositoryConnectorProvider.class, DefaultInstaller.class,
        DefaultDeployer.class, DefaultArtifactResolver.class, DefaultMetadataResolver.class,
        DefaultArtifactDescriptorReader.class, DefaultVersionResolver.class, DefaultVersionRangeResolver.class,
        DefaultModelCacheFactory.class, DefaultDependencyCollector.class, DefaultRepositorySystem.class
})
@Factory
class MavenResolverConfiguration {

    @Bean
    @Named("namedLockFactories")
    Map<String, NamedLockFactory> namedLockFactories() {
        return Map.of(
                NoopNamedLockFactory.NAME, new NoopNamedLockFactory(),
                LocalReadWriteLockNamedLockFactory.NAME, new LocalReadWriteLockNamedLockFactory(),
                LocalSemaphoreNamedLockFactory.NAME, new LocalSemaphoreNamedLockFactory(),
                FileLockNamedLockFactory.NAME, new FileLockNamedLockFactory()
        );
    }

    @Bean
    @Named("nameMappers")
    Map<String, NameMapper> nameMappers() {
        return Map.of(
                NameMappers.STATIC_NAME, NameMappers.staticNameMapper(),
                NameMappers.GAV_NAME, NameMappers.gavNameMapper(),
                NameMappers.DISCRIMINATING_NAME, NameMappers.discriminatingNameMapper(),
                NameMappers.FILE_GAV_NAME, NameMappers.fileGavNameMapper(),
                NameMappers.FILE_HGAV_NAME, NameMappers.fileHashingGavNameMapper()
        );
    }

    @Bean
    @Named("checksumAlgorithmFactories")
    Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories() {
        return Map.of();
    }

    @Bean
    ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector() {
        return new ChecksumAlgorithmFactorySelector() {

            @Override
            public ChecksumAlgorithmFactory select(String s) {
                return null;
            }

            @Override
            public List<ChecksumAlgorithmFactory> selectList(Collection<String> collection) {
                return List.of();
            }

            @Override
            public Collection<ChecksumAlgorithmFactory> getChecksumAlgorithmFactories() {
                return List.of();
            }

            @Override
            public boolean isChecksumExtension(String s) {
                return false;
            }
        };
    }

    @Bean
    @Named("repositoryLayoutFactories")
    RepositoryLayoutFactory maven2RepositoryLayoutFactory(ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        return new Maven2RepositoryLayoutFactory(checksumAlgorithmFactorySelector);
    }

    @Bean
    @Named("remoteRepositoryFilterSources")
    Map<String, RemoteRepositoryFilterSource> remoteRepositoryFilterSources(RepositorySystemLifecycle repositorySystemLifecycle,
                                                                            RepositoryLayoutProvider repositoryLayoutProvider) {
        return Map.of(
                GroupIdRemoteRepositoryFilterSource.NAME,
                new GroupIdRemoteRepositoryFilterSource(repositorySystemLifecycle),
                PrefixesRemoteRepositoryFilterSource.NAME,
                new PrefixesRemoteRepositoryFilterSource(repositoryLayoutProvider)
        );
    }

    @Bean
    @Named("trustedChecksumsSources")
    Map<String, TrustedChecksumsSource> trustedChecksumsSources(FileProcessor fileProcessor,
                                                                LocalPathComposer localPathComposer,
                                                                RepositorySystemLifecycle repositorySystemLifecycle) {
        return Map.of(
                SparseDirectoryTrustedChecksumsSource.NAME,
                new SparseDirectoryTrustedChecksumsSource(fileProcessor, localPathComposer),
                SummaryFileTrustedChecksumsSource.NAME,
                new SummaryFileTrustedChecksumsSource(localPathComposer, repositorySystemLifecycle)
        );
    }

    @Bean
    @Named("providedChecksumsSources")
    Map<String, ProvidedChecksumsSource> providedChecksumsSources(Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        return Map.of(TrustedToProvidedChecksumsSourceAdapter.NAME, new TrustedToProvidedChecksumsSourceAdapter(trustedChecksumsSources));
    }

    @Bean
    @Named("checksumExtractors")
    Map<String, ChecksumExtractor> checksumExtractors() {
        return Map.of(
                Nexus2ChecksumExtractor.NAME, new Nexus2ChecksumExtractor(),
                XChecksumChecksumExtractor.NAME, new XChecksumChecksumExtractor()
        );
    }

    @Bean
    HttpTransporterFactory httpTransportFactory(Map<String, ChecksumExtractor> extractors) {
        return new HttpTransporterFactory(extractors);
    }

    @Bean
    FileTransporterFactory fileTransportFactory() {
        return new FileTransporterFactory();
    }

    @Bean
    @Named("repositoryConnectorFactories")
    Map<String, RepositoryConnectorFactory> repositoryConnectorFactories(BasicRepositoryConnectorFactory basicRepositoryConnectorFactory) {
        return Map.of("basic", basicRepositoryConnectorFactory);
    }

    @Bean
    @Named("dependencyCollectorDelegates")
    Map<String, DependencyCollectorDelegate> dependencyCollectorDelegates(
            RemoteRepositoryManager remoteRepositoryManager,
            ArtifactDescriptorReader artifactDescriptorReader,
            VersionRangeResolver versionRangeResolver) {
        DfDependencyCollector collector1 = new DfDependencyCollector();
        collector1.setRemoteRepositoryManager(remoteRepositoryManager);
        collector1.setArtifactDescriptorReader(artifactDescriptorReader);
        collector1.setVersionRangeResolver(versionRangeResolver);
        BfDependencyCollector collector2 = new BfDependencyCollector();
        collector2.setRemoteRepositoryManager(remoteRepositoryManager);
        collector2.setArtifactDescriptorReader(artifactDescriptorReader);
        collector2.setVersionRangeResolver(versionRangeResolver);
        return Map.of(
                DfDependencyCollector.NAME, collector1,
                BfDependencyCollector.NAME, collector2
        );
    }

    @Bean
    @Named("artifactResolverPostProcessors")
    Map<String, ArtifactResolverPostProcessor> artifactResolverPostProcessors(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector,
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        var processor = new TrustedChecksumsArtifactResolverPostProcessor(checksumAlgorithmFactorySelector, trustedChecksumsSources);
        return Map.of(TrustedChecksumsArtifactResolverPostProcessor.NAME, processor);
    }

    @Bean
    @Named("metadataGeneratorFactories")
    Map<String, MetadataGeneratorFactory> metadataGeneratorFactories() {
        return Map.of(
                "plugins", new PluginsMetadataGeneratorFactory(),
                "versions", new VersionsMetadataGeneratorFactory(),
                "snapshot", new SnapshotMetadataGeneratorFactory()
        );
    }

    @Bean
    ModelBuilder getModelBuilder() {
        return new DefaultModelBuilderFactory().newInstance();
    }

    @Bean
    DefaultRepositorySystemSession defaultRepositorySystemSession(RepositorySystem repositorySystem) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        // Settings this to false might make requests to some strange hosts
        // probably should be switched if supporting multiple repos is the thing
        session.setIgnoreArtifactDescriptorRepositories(true);
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, new LocalRepository(Config.get("maven.local-repo.path"))));
        session.setRepositoryListener(new LoggingRepositoryListener());
        session.setConfigProperty("aether.dependencyCollector.impl", BfDependencyCollector.NAME);
        session.setConfigProperty("aether.dependencyCollector.bf.threads", 32);
        session.setConfigProperty(ArtifactDescriptorReaderDelegate.class.getName(), new CustomDescriptorReaderDelegate());
        // for maven profiles activation which depend on jdk version - value doesn't really matter
        session.setSystemProperties(Map.of(
                "java.version", System.getProperty("java.version"),
                "java.home", System.getProperty("java.home"),
                "os.detected.name", "linux",
                "os.detected.arch", "x86_64",
                "os.detected.classifier", "linux-x86_64"
        ));
        return session;
    }
}
