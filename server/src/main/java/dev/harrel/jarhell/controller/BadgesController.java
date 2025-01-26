package dev.harrel.jarhell.controller;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.analyze.AnalyzeEngine;
import dev.harrel.jarhell.error.BadRequestException;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.ArtifactTree;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.repo.ArtifactRepository;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Controller("/api/v1/badges")
class BadgesController {
    private final ArtifactRepository repo;
    private final MavenApiClient mavenApiClient;
    private final AnalyzeEngine engine;

    BadgesController(ArtifactRepository repo, MavenApiClient mavenApiClient, AnalyzeEngine engine) {
        this.repo = repo;
        this.mavenApiClient = mavenApiClient;
        this.engine = engine;
    }

    // todo: support name & color overrides - maybe even all shields.io config
    @Get("/{metric}/{coordinate}")
    void getMetricBadge(Context ctx, Metric metric, String coordinate) {
        String version;
        String[] split = coordinate.split(":");
        if (split.length == 2) {
            try {
                version = mavenApiClient.fetchArtifactVersions(split[0], split[1]).getLast();
            } catch (Exception e) {
                toBadge(ctx, metric.getName(), "not found", Color.red, Duration.ofDays(7));
                return;
            }
        } else if (split.length == 3) {
            version = split[2];
        } else {
            throw new BadRequestException("Invalid artifact coordinate format [%s]".formatted(coordinate));
        }
        Gav gav = new Gav(split[0], split[1], version);
        ArtifactTree at = repo.find(gav, 0).orElse(null);
        if (at == null) {
            if (mavenApiClient.checkIfArtifactExists(gav)) {
                engine.analyze(gav);
                toBadge(ctx, metric.getName(), "not analyzed", Color.yellow, Duration.ofMinutes(5));
            } else {
                toBadge(ctx, metric.getName(), "not found", Color.red, Duration.ofDays(7));
            }
        } else {
            if (Boolean.TRUE.equals(at.artifactInfo().unresolved())) {
                toBadge(ctx, metric.getName(), "analysis failed", Color.red, Duration.ofDays(1));
            } else {
                toBadge(ctx, metric.getName(), metric.getValue(at.artifactInfo()), metric.getColor(at.artifactInfo()), Duration.ofDays(7));
            }
        }
    }

    private static void toBadge(Context ctx, String name, String value, Color color, Duration cache) {
        ctx.header(Header.CACHE_CONTROL, "max-age=" + cache.toSeconds())
                .redirect("https://shields.io/badge/%s-%s-%s".formatted(escape(name), escape(value), color), HttpStatus.SEE_OTHER);
    }

    private static String escape(String value) {
        return value.replace(' ', '_');
    }

    private final static BigDecimal THOUSAND = new BigDecimal(1_000);
    private final static BigDecimal MILLION = new BigDecimal(1_000_000);
    private final static BigDecimal BILLION = new BigDecimal(1_000_000_000);

    private static String formatBytes(Long bytes) {
        if (bytes < 1_000) {
            return bytes + "B";
        }
        BigDecimal bytesDecimal = new BigDecimal(bytes).setScale(2, RoundingMode.HALF_EVEN);
        if (bytes < 1_000_000) {
            return bytesDecimal.divide(THOUSAND, RoundingMode.HALF_EVEN) + "KB";
        }
        if (bytes < 1_000_000_000) {
            return bytesDecimal.divide(MILLION, RoundingMode.HALF_EVEN) + "MB";
        } else {
            return bytesDecimal.divide(BILLION, RoundingMode.HALF_EVEN) + "GB";
        }
    }

    enum Color {
        brightgreen, yellow, orange, red
    }

    enum Metric {
        size {
            @Override
            String getName() {
                return "package size";
            }

            @Override
            String getValue(ArtifactInfo info) {
                return formatBytes(info.packageSize());
            }

            @Override
            Color getColor(ArtifactInfo info) {
                Long size = info.packageSize();
                if (size <= 300_000) {
                    return Color.brightgreen;
                } else if (size <= 1_000_000) {
                    return Color.yellow;
                } else if (size <= 2_000_000) {
                    return Color.orange;
                } else {
                    return Color.red;
                }
            }
        },
        total_size {
            @Override
            String getName() {
                return "total size";
            }

            @Override
            String getValue(ArtifactInfo info) {
                return formatBytes(info.effectiveValues().size());
            }

            @Override
            Color getColor(ArtifactInfo info) {
                Long size = info.effectiveValues().size();
                if (size <= 1_000_000) {
                    return Color.brightgreen;
                } else if (size <= 3_000_000) {
                    return Color.yellow;
                } else if (size <= 8_000_000) {
                    return Color.orange;
                } else {
                    return Color.red;
                }
            }
        },
        bytecode {
            @Override
            String getName() {
                return "package bytecode version";
            }

            @Override
            String getValue(ArtifactInfo info) {
                // todo
                return "";
            }

            @Override
            Color getColor(ArtifactInfo info) {
                return Color.brightgreen;
            }
        },
        effective_bytecode {
            @Override
            String getName() {
                return "effective bytecode version";
            }

            @Override
            String getValue(ArtifactInfo info) {
                // todo
                return "";
            }

            @Override
            Color getColor(ArtifactInfo info) {
                return Color.brightgreen;
            }
        },
        dependencies {
            @Override
            String getName() {
                return "dependencies";
            }

            @Override
            String getValue(ArtifactInfo info) {
                return info.effectiveValues().requiredDependencies().toString();
            }

            @Override
            Color getColor(ArtifactInfo info) {
                return Color.brightgreen;
            }
        },
        optional_dependencies {
            @Override
            String getName() {
                return "optional dependencies";
            }

            @Override
            String getValue(ArtifactInfo info) {
                return info.effectiveValues().optionalDependencies().toString();
            }

            @Override
            Color getColor(ArtifactInfo info) {
                return Color.brightgreen;
            }
        };

        abstract String getName();

        abstract String getValue(ArtifactInfo info);

        abstract Color getColor(ArtifactInfo info);
    }
}
