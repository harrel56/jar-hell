package dev.harrel.jarhell.model;

public record LicenseCount(LicenseType licenseType, Long count) {
    public static LicenseCount fromString(String value) {
        String[] parts = value.split(";", 2);
        return new LicenseCount(LicenseType.valueOf(parts[0]), Long.valueOf(parts[1]));
    }

    public String asString() {
        return "%s;%s".formatted(licenseType.name(), count);
    }
}
