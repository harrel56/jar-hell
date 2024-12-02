package dev.harrel.jarhell.model.descriptor;

import dev.harrel.jarhell.model.LicenseType;

import java.util.List;

public record DescriptorInfo(String packaging,
                             String name,
                             String description,
                             String url,
                             String scmUrl,
                             String issuesUrl,
                             String inceptionYear,
                             List<License> licenses,
                             List<LicenseType> licenseTypes) {}
