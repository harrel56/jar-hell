package dev.harrel.jarhell.playwright;

import org.neo4j.driver.Driver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Cypher seeding shared by the playwright tests. Everything merges rather than creates, because an analysis
 * kicked off by the previous test may still be running and persist one of these nodes between the database
 * wipe and the call.
 */
final class Fixtures {
    private Fixtures() {}

    /**
     * Seeds versions exactly as the maven index importer would: known to the index, not analysed yet.
     * Sets unconditionally, so that a version an earlier test left analysed is reset rather than inherited.
     */
    static void insertIndexedVersions(Driver driver, String groupId, String artifactId, List<String> versions) {
        String statement = IntStream.range(0, versions.size())
                .mapToObj(i -> """
                        MERGE (v%d:Artifact {groupId: '%s', artifactId: '%s', version: '%s', classifier: ''})
                        SET v%d.fromMavenIndex = true, v%d.unresolved = true, v%d.unresolvedReason = 'initial-indexing'"""
                        .formatted(i, groupId, artifactId, versions.get(i), i, i, i))
                .collect(Collectors.joining("\n"));
        run(driver, statement);
    }

    /** any {@code unresolvedReason} other than {@code initial-indexing} makes the version FAILED rather than NOT_ANALYZED */
    static void insertFailedVersion(Driver driver, String groupId, String artifactId, String version,
                                    String reason, int attempts) {
        run(driver, """
                MERGE (v:Artifact {groupId: '%s', artifactId: '%s', version: '%s', classifier: ''})
                ON CREATE SET v.fromMavenIndex = true, v.unresolved = true, v.unresolvedReason = '%s',
                              v.unresolvedCount = %d, v.analyzed = localdatetime()"""
                .formatted(groupId, artifactId, version, reason, attempts));
    }

    static void run(Driver driver, String statement) {
        try (var session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run(statement));
        }
    }
}
