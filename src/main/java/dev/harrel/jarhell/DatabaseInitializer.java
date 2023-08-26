package dev.harrel.jarhell;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String UNIQUE_GAV_WITH_CLASSIFIER = """
                    CREATE CONSTRAINT unique_gav IF NOT EXISTS
                    FOR (n:Artifact)
                    REQUIRE (n.groupId, n.artifactId, n.version, n.classifier) IS UNIQUE
                    """;
    private static final String INDEX_GAV = """
                    CREATE INDEX index_gav IF NOT EXISTS
                    FOR (n:Artifact)
                    ON (n.groupId, n.artifactId, n.version)
                    """;

    public static void initialize(Driver driver) {
        try (var session = driver.session()) {
            runDdl(session, UNIQUE_GAV_WITH_CLASSIFIER);
            runDdl(session, INDEX_GAV);
        }
    }

    private static void runDdl(Session session, String query) {
        ResultSummary summary = session.executeWrite(tx -> tx.run(new Query(query)).consume());
        if (summary.counters().containsUpdates()) {
            logger.info("DDL query successful: \n{}", summary.query().text());
        }
    }
}
