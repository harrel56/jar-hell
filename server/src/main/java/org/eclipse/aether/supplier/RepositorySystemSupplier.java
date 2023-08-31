/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.aether.supplier;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.*;
import org.eclipse.aether.RepositoryListener;
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
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactory;
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
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.ChecksumExtractor;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.transport.http.Nexus2ChecksumExtractor;
import org.eclipse.aether.transport.http.XChecksumChecksumExtractor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A simple {@link Supplier} of {@link RepositorySystem} instances, that on each call supplies newly
 * constructed instance. For proper shut down, use {@link RepositorySystem#shutdown()} method on supplied instance(s).
 * <p>
 * Extend this class and override methods to customize, if needed.
 *
 * @since 1.9.15
 */
public class RepositorySystemSupplier implements Supplier<RepositorySystem> {
    protected FileProcessor getFileProcessor() {
        return new DefaultFileProcessor();
    }

    protected TrackingFileManager getTrackingFileManager() {
        return new DefaultTrackingFileManager();
    }

    protected LocalPathComposer getLocalPathComposer() {
        return new DefaultLocalPathComposer();
    }

    protected LocalPathPrefixComposerFactory getLocalPathPrefixComposerFactory() {
        return new DefaultLocalPathPrefixComposerFactory();
    }

    protected RepositorySystemLifecycle getRepositorySystemLifecycle() {
        return new DefaultRepositorySystemLifecycle();
    }

    protected OfflineController getOfflineController() {
        return new DefaultOfflineController();
    }

    protected UpdatePolicyAnalyzer getUpdatePolicyAnalyzer() {
        return new DefaultUpdatePolicyAnalyzer();
    }

    protected ChecksumPolicyProvider getChecksumPolicyProvider() {
        return new DefaultChecksumPolicyProvider();
    }

    protected UpdateCheckManager getUpdateCheckManager(
            TrackingFileManager trackingFileManager, UpdatePolicyAnalyzer updatePolicyAnalyzer) {
        DefaultUpdateCheckManager manager = new DefaultUpdateCheckManager();
        manager.setTrackingFileManager(trackingFileManager);
        manager.setUpdatePolicyAnalyzer(updatePolicyAnalyzer);
        return manager;
    }

    protected Map<String, NamedLockFactory> getNamedLockFactories() {
        HashMap<String, NamedLockFactory> result = new HashMap<>();
        result.put(NoopNamedLockFactory.NAME, new NoopNamedLockFactory());
        result.put(LocalReadWriteLockNamedLockFactory.NAME, new LocalReadWriteLockNamedLockFactory());
        result.put(LocalSemaphoreNamedLockFactory.NAME, new LocalSemaphoreNamedLockFactory());
        result.put(FileLockNamedLockFactory.NAME, new FileLockNamedLockFactory());
        return result;
    }

    protected Map<String, NameMapper> getNameMappers() {
        HashMap<String, NameMapper> result = new HashMap<>();
        result.put(NameMappers.STATIC_NAME, NameMappers.staticNameMapper());
        result.put(NameMappers.GAV_NAME, NameMappers.gavNameMapper());
        result.put(NameMappers.DISCRIMINATING_NAME, NameMappers.discriminatingNameMapper());
        result.put(NameMappers.FILE_GAV_NAME, NameMappers.fileGavNameMapper());
        result.put(NameMappers.FILE_HGAV_NAME, NameMappers.fileHashingGavNameMapper());
        return result;
    }

    protected NamedLockFactoryAdapterFactory getNamedLockFactoryAdapterFactory(
            Map<String, NamedLockFactory> namedLockFactories,
            Map<String, NameMapper> nameMappers,
            RepositorySystemLifecycle repositorySystemLifecycle) {
        return new NamedLockFactoryAdapterFactoryImpl(namedLockFactories, nameMappers, repositorySystemLifecycle);
    }

    protected SyncContextFactory getSyncContextFactory(NamedLockFactoryAdapterFactory namedLockFactoryAdapterFactory) {
        return new DefaultSyncContextFactory(namedLockFactoryAdapterFactory);
    }

    protected Map<String, ChecksumAlgorithmFactory> getChecksumAlgorithmFactories() {
        HashMap<String, ChecksumAlgorithmFactory> result = new HashMap<>();
        result.put(Sha512ChecksumAlgorithmFactory.NAME, new Sha512ChecksumAlgorithmFactory());
        result.put(Sha256ChecksumAlgorithmFactory.NAME, new Sha256ChecksumAlgorithmFactory());
        result.put(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory());
        result.put(Md5ChecksumAlgorithmFactory.NAME, new Md5ChecksumAlgorithmFactory());
        return result;
    }

    protected ChecksumAlgorithmFactorySelector getChecksumAlgorithmFactorySelector(
            Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        return new DefaultChecksumAlgorithmFactorySelector(checksumAlgorithmFactories);
    }

    protected Map<String, RepositoryLayoutFactory> getRepositoryLayoutFactories(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        HashMap<String, RepositoryLayoutFactory> result = new HashMap<>();
        result.put("maven2", new Maven2RepositoryLayoutFactory(checksumAlgorithmFactorySelector));
        return result;
    }

    protected RepositoryLayoutProvider getRepositoryLayoutProvider(
            Map<String, RepositoryLayoutFactory> repositoryLayoutFactories) {
        DefaultRepositoryLayoutProvider provider = new DefaultRepositoryLayoutProvider();
        provider.setRepositoryLayoutFactories(new HashSet<>(repositoryLayoutFactories.values()));
        return provider;
    }

    protected LocalRepositoryProvider getLocalRepositoryProvider(
            LocalPathComposer localPathComposer,
            TrackingFileManager trackingFileManager,
            LocalPathPrefixComposerFactory localPathPrefixComposerFactory) {
        HashSet<LocalRepositoryManagerFactory> localRepositoryProviders = new HashSet<>(2);
        localRepositoryProviders.add(new SimpleLocalRepositoryManagerFactory(localPathComposer));
        localRepositoryProviders.add(new EnhancedLocalRepositoryManagerFactory(
                localPathComposer, trackingFileManager, localPathPrefixComposerFactory));
        DefaultLocalRepositoryProvider provider = new DefaultLocalRepositoryProvider();
        provider.setLocalRepositoryManagerFactories(localRepositoryProviders);
        return provider;
    }

    protected RemoteRepositoryManager getRemoteRepositoryManager(
            UpdatePolicyAnalyzer updatePolicyAnalyzer, ChecksumPolicyProvider checksumPolicyProvider) {
        DefaultRemoteRepositoryManager manager = new DefaultRemoteRepositoryManager();
        manager.setUpdatePolicyAnalyzer(updatePolicyAnalyzer);
        manager.setChecksumPolicyProvider(checksumPolicyProvider);
        return manager;
    }

    protected Map<String, RemoteRepositoryFilterSource> getRemoteRepositoryFilterSources(
            RepositorySystemLifecycle repositorySystemLifecycle, RepositoryLayoutProvider repositoryLayoutProvider) {
        HashMap<String, RemoteRepositoryFilterSource> result = new HashMap<>();
        result.put(
                GroupIdRemoteRepositoryFilterSource.NAME,
                new GroupIdRemoteRepositoryFilterSource(repositorySystemLifecycle));
        result.put(
                PrefixesRemoteRepositoryFilterSource.NAME,
                new PrefixesRemoteRepositoryFilterSource(repositoryLayoutProvider));
        return result;
    }

    protected RemoteRepositoryFilterManager getRemoteRepositoryFilterManager(
            Map<String, RemoteRepositoryFilterSource> remoteRepositoryFilterSources) {
        return new DefaultRemoteRepositoryFilterManager(remoteRepositoryFilterSources);
    }

    protected Map<String, RepositoryListener> getRepositoryListeners() {
        return new HashMap<>();
    }

    protected RepositoryEventDispatcher getRepositoryEventDispatcher(
            Map<String, RepositoryListener> repositoryListeners) {
        DefaultRepositoryEventDispatcher dispatcher = new DefaultRepositoryEventDispatcher();
        dispatcher.setRepositoryListeners(new HashSet<>(repositoryListeners.values()));
        return dispatcher;
    }

    protected Map<String, TrustedChecksumsSource> getTrustedChecksumsSources(
            FileProcessor fileProcessor,
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

    protected Map<String, ProvidedChecksumsSource> getProvidedChecksumsSources(
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        HashMap<String, ProvidedChecksumsSource> result = new HashMap<>();
        result.put(
                TrustedToProvidedChecksumsSourceAdapter.NAME,
                new TrustedToProvidedChecksumsSourceAdapter(trustedChecksumsSources));
        return result;
    }

    protected Map<String, ChecksumExtractor> getChecksumExtractors() {
        HashMap<String, ChecksumExtractor> result = new HashMap<>();
        result.put(Nexus2ChecksumExtractor.NAME, new Nexus2ChecksumExtractor());
        result.put(XChecksumChecksumExtractor.NAME, new XChecksumChecksumExtractor());
        return result;
    }

    protected Map<String, TransporterFactory> getTransporterFactories(Map<String, ChecksumExtractor> extractors) {
        HashMap<String, TransporterFactory> result = new HashMap<>();
        result.put("file", new FileTransporterFactory());
        result.put("http", new HttpTransporterFactory(extractors));
        return result;
    }

    protected TransporterProvider getTransporterProvider(Map<String, TransporterFactory> transporterFactories) {
        DefaultTransporterProvider provider = new DefaultTransporterProvider();
        provider.setTransporterFactories(new HashSet<>(transporterFactories.values()));
        return provider;
    }

    protected BasicRepositoryConnectorFactory getBasicRepositoryConnectorFactory(
            TransporterProvider transporterProvider,
            RepositoryLayoutProvider repositoryLayoutProvider,
            ChecksumPolicyProvider checksumPolicyProvider,
            FileProcessor fileProcessor,
            Map<String, ProvidedChecksumsSource> providedChecksumsSources) {
        return new BasicRepositoryConnectorFactory(
                transporterProvider,
                repositoryLayoutProvider,
                checksumPolicyProvider,
                fileProcessor,
                providedChecksumsSources);
    }

    protected Map<String, RepositoryConnectorFactory> getRepositoryConnectorFactories(
            BasicRepositoryConnectorFactory basicRepositoryConnectorFactory) {
        HashMap<String, RepositoryConnectorFactory> result = new HashMap<>();
        result.put("basic", basicRepositoryConnectorFactory);
        return result;
    }

    protected RepositoryConnectorProvider getRepositoryConnectorProvider(
            Map<String, RepositoryConnectorFactory> repositoryConnectorFactories,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        DefaultRepositoryConnectorProvider provider = new DefaultRepositoryConnectorProvider();
        provider.setRepositoryConnectorFactories(new HashSet<>(repositoryConnectorFactories.values()));
        provider.setRemoteRepositoryFilterManager(remoteRepositoryFilterManager);
        return provider;
    }

    protected Installer getInstaller(
            FileProcessor fileProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            Map<String, MetadataGeneratorFactory> metadataGeneratorFactories,
            SyncContextFactory syncContextFactory) {
        DefaultInstaller installer = new DefaultInstaller();
        installer.setFileProcessor(fileProcessor);
        installer.setRepositoryEventDispatcher(repositoryEventDispatcher);
        installer.setMetadataGeneratorFactories(new HashSet<>(metadataGeneratorFactories.values()));
        installer.setSyncContextFactory(syncContextFactory);
        return installer;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    protected Deployer getDeployer(
            FileProcessor fileProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            UpdateCheckManager updateCheckManager,
            Map<String, MetadataGeneratorFactory> metadataGeneratorFactories,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController) {
        DefaultDeployer deployer = new DefaultDeployer();
        deployer.setFileProcessor(fileProcessor);
        deployer.setRepositoryEventDispatcher(repositoryEventDispatcher);
        deployer.setRepositoryConnectorProvider(repositoryConnectorProvider);
        deployer.setRemoteRepositoryManager(remoteRepositoryManager);
        deployer.setUpdateCheckManager(updateCheckManager);
        deployer.setMetadataGeneratorFactories(new HashSet<>(metadataGeneratorFactories.values()));
        deployer.setSyncContextFactory(syncContextFactory);
        deployer.setOfflineController(offlineController);
        return deployer;
    }

    protected Map<String, DependencyCollectorDelegate> getDependencyCollectorDelegates(
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

    protected DependencyCollector getDependencyCollector(
            Map<String, DependencyCollectorDelegate> dependencyCollectorDelegates) {
        return new DefaultDependencyCollector(dependencyCollectorDelegates);
    }

    protected Map<String, ArtifactResolverPostProcessor> getArtifactResolverPostProcessors(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector,
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        HashMap<String, ArtifactResolverPostProcessor> result = new HashMap<>();
        result.put(
                TrustedChecksumsArtifactResolverPostProcessor.NAME,
                new TrustedChecksumsArtifactResolverPostProcessor(
                        checksumAlgorithmFactorySelector, trustedChecksumsSources));
        return result;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    protected ArtifactResolver getArtifactResolver(
            FileProcessor fileProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            VersionResolver versionResolver,
            UpdateCheckManager updateCheckManager,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController,
            Map<String, ArtifactResolverPostProcessor> artifactResolverPostProcessors,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        DefaultArtifactResolver resolver = new DefaultArtifactResolver();
        resolver.setFileProcessor(fileProcessor);
        resolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        resolver.setVersionResolver(versionResolver);
        resolver.setUpdateCheckManager(updateCheckManager);
        resolver.setRepositoryConnectorProvider(repositoryConnectorProvider);
        resolver.setRemoteRepositoryManager(remoteRepositoryManager);
        resolver.setSyncContextFactory(syncContextFactory);
        resolver.setOfflineController(offlineController);
        resolver.setArtifactResolverPostProcessors(artifactResolverPostProcessors);
        resolver.setRemoteRepositoryFilterManager(remoteRepositoryFilterManager);
        return resolver;
    }

    protected MetadataResolver getMetadataResolver(
            RepositoryEventDispatcher repositoryEventDispatcher,
            UpdateCheckManager updateCheckManager,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        DefaultMetadataResolver resolver = new DefaultMetadataResolver();
        resolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        resolver.setUpdateCheckManager(updateCheckManager);
        resolver.setRepositoryConnectorProvider(repositoryConnectorProvider);
        resolver.setRemoteRepositoryManager(remoteRepositoryManager);
        resolver.setSyncContextFactory(syncContextFactory);
        resolver.setOfflineController(offlineController);
        resolver.setRemoteRepositoryFilterManager(remoteRepositoryFilterManager);
        return resolver;
    }

    // Maven provided

    protected Map<String, MetadataGeneratorFactory> getMetadataGeneratorFactories() {
        // from maven-resolver-provider
        HashMap<String, MetadataGeneratorFactory> result = new HashMap<>();
        result.put("plugins", new PluginsMetadataGeneratorFactory());
        result.put("versions", new VersionsMetadataGeneratorFactory());
        result.put("snapshot", new SnapshotMetadataGeneratorFactory());
        return result;
    }

    protected ArtifactDescriptorReader getArtifactDescriptorReader(
            RemoteRepositoryManager remoteRepositoryManager,
            VersionResolver versionResolver,
            VersionRangeResolver versionRangeResolver,
            ArtifactResolver artifactResolver,
            ModelBuilder modelBuilder,
            RepositoryEventDispatcher repositoryEventDispatcher,
            ModelCacheFactory modelCacheFactory) {
        // from maven-resolver-provider
        DefaultArtifactDescriptorReader result = new DefaultArtifactDescriptorReader();
        result.setRemoteRepositoryManager(remoteRepositoryManager);
        result.setVersionResolver(versionResolver);
        result.setVersionRangeResolver(versionRangeResolver);
        result.setArtifactResolver(artifactResolver);
        result.setModelBuilder(modelBuilder);
        result.setRepositoryEventDispatcher(repositoryEventDispatcher);
        result.setModelCacheFactory(modelCacheFactory);
        return result;
    }

    protected VersionResolver getVersionResolver(
            MetadataResolver metadataResolver,
            SyncContextFactory syncContextFactory,
            RepositoryEventDispatcher repositoryEventDispatcher) {
        // from maven-resolver-provider
        DefaultVersionResolver result = new DefaultVersionResolver();
        result.setMetadataResolver(metadataResolver);
        result.setSyncContextFactory(syncContextFactory);
        result.setRepositoryEventDispatcher(repositoryEventDispatcher);
        return result;
    }

    protected VersionRangeResolver getVersionRangeResolver(
            MetadataResolver metadataResolver,
            SyncContextFactory syncContextFactory,
            RepositoryEventDispatcher repositoryEventDispatcher) {
        // from maven-resolver-provider
        DefaultVersionRangeResolver result = new DefaultVersionRangeResolver();
        result.setMetadataResolver(metadataResolver);
        result.setSyncContextFactory(syncContextFactory);
        result.setRepositoryEventDispatcher(repositoryEventDispatcher);
        return result;
    }

    protected ModelBuilder getModelBuilder() {
        // from maven-model-builder
        return new DefaultModelBuilderFactory().newInstance();
    }

    protected ModelCacheFactory getModelCacheFactory() {
        // from maven-resolver-provider
        return new DefaultModelCacheFactory();
    }

    @Override
    public RepositorySystem get() {
        FileProcessor fileProcessor = getFileProcessor();
        TrackingFileManager trackingFileManager = getTrackingFileManager();
        LocalPathComposer localPathComposer = getLocalPathComposer();
        LocalPathPrefixComposerFactory localPathPrefixComposerFactory = getLocalPathPrefixComposerFactory();
        RepositorySystemLifecycle repositorySystemLifecycle = getRepositorySystemLifecycle();
        OfflineController offlineController = getOfflineController();
        UpdatePolicyAnalyzer updatePolicyAnalyzer = getUpdatePolicyAnalyzer();
        ChecksumPolicyProvider checksumPolicyProvider = getChecksumPolicyProvider();

        UpdateCheckManager updateCheckManager = getUpdateCheckManager(trackingFileManager, updatePolicyAnalyzer);

        Map<String, NamedLockFactory> namedLockFactories = getNamedLockFactories();
        Map<String, NameMapper> nameMappers = getNameMappers();
        NamedLockFactoryAdapterFactory namedLockFactoryAdapterFactory =
                getNamedLockFactoryAdapterFactory(namedLockFactories, nameMappers, repositorySystemLifecycle);
        SyncContextFactory syncContextFactory = getSyncContextFactory(namedLockFactoryAdapterFactory);

        Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories = getChecksumAlgorithmFactories();
        ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector =
                getChecksumAlgorithmFactorySelector(checksumAlgorithmFactories);

        Map<String, RepositoryLayoutFactory> repositoryLayoutFactories =
                getRepositoryLayoutFactories(checksumAlgorithmFactorySelector);
        RepositoryLayoutProvider repositoryLayoutProvider = getRepositoryLayoutProvider(repositoryLayoutFactories);

        LocalRepositoryProvider localRepositoryProvider =
                getLocalRepositoryProvider(localPathComposer, trackingFileManager, localPathPrefixComposerFactory);

        RemoteRepositoryManager remoteRepositoryManager =
                getRemoteRepositoryManager(updatePolicyAnalyzer, checksumPolicyProvider);
        Map<String, RemoteRepositoryFilterSource> remoteRepositoryFilterSources =
                getRemoteRepositoryFilterSources(repositorySystemLifecycle, repositoryLayoutProvider);
        RemoteRepositoryFilterManager remoteRepositoryFilterManager =
                getRemoteRepositoryFilterManager(remoteRepositoryFilterSources);

        Map<String, RepositoryListener> repositoryListeners = getRepositoryListeners();
        RepositoryEventDispatcher repositoryEventDispatcher = getRepositoryEventDispatcher(repositoryListeners);

        Map<String, TrustedChecksumsSource> trustedChecksumsSources =
                getTrustedChecksumsSources(fileProcessor, localPathComposer, repositorySystemLifecycle);
        Map<String, ProvidedChecksumsSource> providedChecksumsSources =
                getProvidedChecksumsSources(trustedChecksumsSources);

        Map<String, ChecksumExtractor> checksumExtractors = getChecksumExtractors();
        Map<String, TransporterFactory> transporterFactories = getTransporterFactories(checksumExtractors);
        TransporterProvider transporterProvider = getTransporterProvider(transporterFactories);

        BasicRepositoryConnectorFactory basic = getBasicRepositoryConnectorFactory(
                transporterProvider,
                repositoryLayoutProvider,
                checksumPolicyProvider,
                fileProcessor,
                providedChecksumsSources);
        Map<String, RepositoryConnectorFactory> repositoryConnectorFactories = getRepositoryConnectorFactories(basic);
        RepositoryConnectorProvider repositoryConnectorProvider =
                getRepositoryConnectorProvider(repositoryConnectorFactories, remoteRepositoryFilterManager);

        Map<String, MetadataGeneratorFactory> metadataGeneratorFactories = getMetadataGeneratorFactories();

        Installer installer =
                getInstaller(fileProcessor, repositoryEventDispatcher, metadataGeneratorFactories, syncContextFactory);
        Deployer deployer = getDeployer(
                fileProcessor,
                repositoryEventDispatcher,
                repositoryConnectorProvider,
                remoteRepositoryManager,
                updateCheckManager,
                metadataGeneratorFactories,
                syncContextFactory,
                offlineController);

        MetadataResolver metadataResolver = getMetadataResolver(
                repositoryEventDispatcher,
                updateCheckManager,
                repositoryConnectorProvider,
                remoteRepositoryManager,
                syncContextFactory,
                offlineController,
                remoteRepositoryFilterManager);

        VersionResolver versionResolver =
                getVersionResolver(metadataResolver, syncContextFactory, repositoryEventDispatcher);
        VersionRangeResolver versionRangeResolver =
                getVersionRangeResolver(metadataResolver, syncContextFactory, repositoryEventDispatcher);

        Map<String, ArtifactResolverPostProcessor> artifactResolverPostProcessors =
                getArtifactResolverPostProcessors(checksumAlgorithmFactorySelector, trustedChecksumsSources);
        ArtifactResolver artifactResolver = getArtifactResolver(
                fileProcessor,
                repositoryEventDispatcher,
                versionResolver,
                updateCheckManager,
                repositoryConnectorProvider,
                remoteRepositoryManager,
                syncContextFactory,
                offlineController,
                artifactResolverPostProcessors,
                remoteRepositoryFilterManager);

        ModelBuilder modelBuilder = getModelBuilder();
        ModelCacheFactory modelCacheFactory = getModelCacheFactory();

        ArtifactDescriptorReader artifactDescriptorReader = getArtifactDescriptorReader(
                remoteRepositoryManager,
                versionResolver,
                versionRangeResolver,
                artifactResolver,
                modelBuilder,
                repositoryEventDispatcher,
                modelCacheFactory);

        Map<String, DependencyCollectorDelegate> dependencyCollectorDelegates = getDependencyCollectorDelegates(
                remoteRepositoryManager, artifactDescriptorReader, versionRangeResolver);
        DependencyCollector dependencyCollector = getDependencyCollector(dependencyCollectorDelegates);

        DefaultRepositorySystem system = new DefaultRepositorySystem();
        system.setVersionResolver(versionResolver);
        system.setVersionRangeResolver(versionRangeResolver);
        system.setArtifactResolver(artifactResolver);
        system.setMetadataResolver(metadataResolver);
        system.setArtifactDescriptorReader(artifactDescriptorReader);
        system.setDependencyCollector(dependencyCollector);
        system.setInstaller(installer);
        system.setDeployer(deployer);
        system.setLocalRepositoryProvider(localRepositoryProvider);
        system.setSyncContextFactory(syncContextFactory);
        system.setRemoteRepositoryManager(remoteRepositoryManager);
        system.setRepositorySystemLifecycle(repositorySystemLifecycle);
        return system;
    }
}