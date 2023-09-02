package dev.harrel.jarhell;

import io.avaje.inject.Bean;
import io.avaje.inject.Component;
import io.avaje.inject.Factory;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.*;
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
import org.eclipse.aether.spi.checksums.ProvidedChecksumsSource;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.ChecksumExtractor;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.transport.http.Nexus2ChecksumExtractor;
import org.eclipse.aether.transport.http.XChecksumChecksumExtractor;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Component.Import({
        DefaultFileProcessor.class, DefaultTrackingFileManager.class, DefaultChecksumAlgorithmFactorySelector.class,
        DefaultLocalPathComposer.class, DefaultLocalPathPrefixComposerFactory.class, DefaultRepositorySystemLifecycle.class,
        DefaultOfflineController.class, DefaultUpdatePolicyAnalyzer.class, DefaultChecksumPolicyProvider.class,
        DefaultUpdateCheckManager.class, NamedLockFactoryAdapterFactoryImpl.class, DefaultSyncContextFactory.class,
        DefaultChecksumAlgorithmFactorySelector.class, DefaultRepositoryLayoutProvider.class, SimpleLocalRepositoryManagerFactory.class,
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
        HashMap<String, NamedLockFactory> result = new HashMap<>();
        result.put(NoopNamedLockFactory.NAME, new NoopNamedLockFactory());
        result.put(LocalReadWriteLockNamedLockFactory.NAME, new LocalReadWriteLockNamedLockFactory());
        result.put(LocalSemaphoreNamedLockFactory.NAME, new LocalSemaphoreNamedLockFactory());
        result.put(FileLockNamedLockFactory.NAME, new FileLockNamedLockFactory());
        return result;
    }

    @Bean
    @Named("nameMappers")
    Map<String, NameMapper> nameMappers() {
        HashMap<String, NameMapper> result = new HashMap<>();
        result.put(NameMappers.STATIC_NAME, NameMappers.staticNameMapper());
        result.put(NameMappers.GAV_NAME, NameMappers.gavNameMapper());
        result.put(NameMappers.DISCRIMINATING_NAME, NameMappers.discriminatingNameMapper());
        result.put(NameMappers.FILE_GAV_NAME, NameMappers.fileGavNameMapper());
        result.put(NameMappers.FILE_HGAV_NAME, NameMappers.fileHashingGavNameMapper());
        return result;
    }

    @Bean
    @Named("checksumAlgorithmFactories")
    Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories() {
        HashMap<String, ChecksumAlgorithmFactory> result = new HashMap<>();
        result.put(Sha512ChecksumAlgorithmFactory.NAME, new Sha512ChecksumAlgorithmFactory());
        result.put(Sha256ChecksumAlgorithmFactory.NAME, new Sha256ChecksumAlgorithmFactory());
        result.put(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory());
        result.put(Md5ChecksumAlgorithmFactory.NAME, new Md5ChecksumAlgorithmFactory());
        return result;
    }

    @Bean
    @Named("repositoryLayoutFactories")
    Map<String, RepositoryLayoutFactory> repositoryLayoutFactories(ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        return Map.of("maven2", new Maven2RepositoryLayoutFactory(checksumAlgorithmFactorySelector));
    }

    @Bean
    @Named("remoteRepositoryFilterSources")
    Map<String, RemoteRepositoryFilterSource> remoteRepositoryFilterSources(RepositorySystemLifecycle repositorySystemLifecycle,
                                                                            RepositoryLayoutProvider repositoryLayoutProvider) {
        HashMap<String, RemoteRepositoryFilterSource> result = new HashMap<>();
        result.put(
                GroupIdRemoteRepositoryFilterSource.NAME,
                new GroupIdRemoteRepositoryFilterSource(repositorySystemLifecycle));
        result.put(
                PrefixesRemoteRepositoryFilterSource.NAME,
                new PrefixesRemoteRepositoryFilterSource(repositoryLayoutProvider));
        return result;
    }

    @Bean
    @Named("trustedChecksumsSources")
    Map<String, TrustedChecksumsSource> trustedChecksumsSources(FileProcessor fileProcessor,
                                                                LocalPathComposer localPathComposer,
                                                                RepositorySystemLifecycle repositorySystemLifecycle) {
        HashMap<String, TrustedChecksumsSource> result = new HashMap<>();
        result.put(
                SparseDirectoryTrustedChecksumsSource.NAME,
                new SparseDirectoryTrustedChecksumsSource(fileProcessor, localPathComposer));
        result.put(
                SummaryFileTrustedChecksumsSource.NAME,
                new SummaryFileTrustedChecksumsSource(localPathComposer, repositorySystemLifecycle));
        return result;
    }

    @Bean
    @Named("providedChecksumsSources")
    Map<String, ProvidedChecksumsSource> providedChecksumsSources(
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        HashMap<String, ProvidedChecksumsSource> result = new HashMap<>();
        result.put(
                TrustedToProvidedChecksumsSourceAdapter.NAME,
                new TrustedToProvidedChecksumsSourceAdapter(trustedChecksumsSources));
        return result;
    }

    @Bean
    @Named("checksumExtractors")
    Map<String, ChecksumExtractor> checksumExtractors() {
        HashMap<String, ChecksumExtractor> result = new HashMap<>();
        result.put(Nexus2ChecksumExtractor.NAME, new Nexus2ChecksumExtractor());
        result.put(XChecksumChecksumExtractor.NAME, new XChecksumChecksumExtractor());
        return result;
    }

    @Bean
    @Named("transporterFactories")
    Map<String, TransporterFactory> transporterFactories(Map<String, ChecksumExtractor> extractors) {
        HashMap<String, TransporterFactory> result = new HashMap<>();
        result.put("file", new FileTransporterFactory());
        result.put("http", new HttpTransporterFactory(extractors));
        return result;
    }

    @Bean
    @Named("repositoryConnectorFactories")
    Map<String, RepositoryConnectorFactory> repositoryConnectorFactories(
            BasicRepositoryConnectorFactory basicRepositoryConnectorFactory) {
        HashMap<String, RepositoryConnectorFactory> result = new HashMap<>();
        result.put("basic", basicRepositoryConnectorFactory);
        return result;
    }

    @Bean
    @Named("dependencyCollectorDelegates")
    Map<String, DependencyCollectorDelegate> dependencyCollectorDelegates(
            RemoteRepositoryManager remoteRepositoryManager,
            ArtifactDescriptorReader artifactDescriptorReader,
            VersionRangeResolver versionRangeResolver) {
        HashMap<String, DependencyCollectorDelegate> result = new HashMap<>();
        DfDependencyCollector collector1 = new DfDependencyCollector();
        collector1.setRemoteRepositoryManager(remoteRepositoryManager);
        collector1.setArtifactDescriptorReader(artifactDescriptorReader);
        collector1.setVersionRangeResolver(versionRangeResolver);
        result.put(
                DfDependencyCollector.NAME,
                collector1);
        BfDependencyCollector collector2 = new BfDependencyCollector();
        collector2.setRemoteRepositoryManager(remoteRepositoryManager);
        collector2.setArtifactDescriptorReader(artifactDescriptorReader);
        collector2.setVersionRangeResolver(versionRangeResolver);
        result.put(
                BfDependencyCollector.NAME,
                collector2);
        return result;
    }

    @Bean
    @Named("artifactResolverPostProcessors")
    Map<String, ArtifactResolverPostProcessor> artifactResolverPostProcessors(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector,
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        HashMap<String, ArtifactResolverPostProcessor> result = new HashMap<>();
        result.put(
                TrustedChecksumsArtifactResolverPostProcessor.NAME,
                new TrustedChecksumsArtifactResolverPostProcessor(
                        checksumAlgorithmFactorySelector, trustedChecksumsSources));
        return result;
    }

    @Bean
    @Named("metadataGeneratorFactories")
    Map<String, MetadataGeneratorFactory> metadataGeneratorFactories() {
        HashMap<String, MetadataGeneratorFactory> result = new HashMap<>();
        result.put("plugins", new PluginsMetadataGeneratorFactory());
        result.put("versions", new VersionsMetadataGeneratorFactory());
        result.put("snapshot", new SnapshotMetadataGeneratorFactory());
        return result;
    }

    @Bean
    ModelBuilder getModelBuilder() {
        return new DefaultModelBuilderFactory().newInstance();
    }
}
