package dev.harrel.jarhell.maven;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingRepositoryListener extends AbstractRepositoryListener {
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
