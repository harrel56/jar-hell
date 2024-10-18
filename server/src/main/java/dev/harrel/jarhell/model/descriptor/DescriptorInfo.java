package dev.harrel.jarhell.model.descriptor;




import org.eclipse.aether.graph.Dependency;

import java.util.List;

public record DescriptorInfo(String packaging,
                             String name,
                             String description,
                             String url,
                             String inceptionYear,
                             List<Licence> licences,
                             List<Dependency> dependencies) {}
