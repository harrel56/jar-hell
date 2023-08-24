package dev.harrel.jarhell;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    public static void initialize(Driver driver) {
        try (var session = driver.session()) {
            ResultSummary summary = session.executeWrite(tx -> tx.run(new Query("""
                    CREATE CONSTRAINT unique_gav IF NOT EXISTS
                    FOR (n:Artifact)
                    REQUIRE (n.groupId, n.artifactId, n.version, n.classifier) IS UNIQUE""")).consume());
            if (summary.counters().containsUpdates()) {
                logger.info("DDL query successful: \n{}", summary.query().text());
            }
        }
    }
}
