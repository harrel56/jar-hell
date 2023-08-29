package org.eclipse.aether.supplier;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.ArtifactDescriptorReaderDelegate;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import java.util.Map;

public class CustomDescriptorReaderDelegate extends ArtifactDescriptorReaderDelegate {
    public static final String MODEL_KEY = "projectModel";

    @Override
    public void populateResult(RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
        super.populateResult(session, result, model);

        result.setProperties(Map.of(MODEL_KEY, model));
    }
}
