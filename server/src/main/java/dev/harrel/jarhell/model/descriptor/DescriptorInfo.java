package dev.harrel.jarhell.model.descriptor;


import java.util.List;

public record DescriptorInfo(String packaging,
                             String name,
                             String description,
                             String url,
                             String inceptionYear,
                             List<Licence> licences) {}
