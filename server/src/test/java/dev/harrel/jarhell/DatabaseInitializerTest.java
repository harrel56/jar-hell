package dev.harrel.jarhell;

import dev.harrel.jarhell.extension.EnvironmentExtension;
import dev.harrel.jarhell.extension.EnvironmentTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.EagerResult;
import org.neo4j.driver.Record;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnvironmentTest
class DatabaseInitializerTest {
    private final Driver driver;

    DatabaseInitializerTest(Driver driver) {
        this.driver = driver;
    }

    @BeforeEach
    void setUp() {
        EnvironmentExtension.clearDatabase(driver);
    }

    @Test
    void shouldInitializeDbCorrectly() {
        DatabaseInitializer.initialize(driver);

        EagerResult indexGavResult = driver.executableQuery("SHOW INDEXES WHERE name = 'index_gav'").execute();
        assertIndex(indexGavResult, List.of("groupId", "artifactId", "version"));

        EagerResult uniqueGavResult = driver.executableQuery("SHOW INDEXES WHERE name = 'unique_gav'").execute();
        assertIndex(uniqueGavResult, List.of("groupId", "artifactId", "version", "classifier"));

        EagerResult uniqueConstraintResult = driver.executableQuery("SHOW CONSTRAINTS").execute();
        assertThat(uniqueConstraintResult.records()).hasSize(1);
        Record constraintRecord = uniqueConstraintResult.records().getFirst();
        assertThat(constraintRecord.get("name").asString()).isEqualTo("unique_gav");
        assertThat(constraintRecord.get("type").asString()).isEqualTo("UNIQUENESS");
        assertThat(constraintRecord.get("entityType").asString()).isEqualTo("NODE");
        assertThat(constraintRecord.get("labelsOrTypes").asList()).isEqualTo(List.of("Artifact"));
        assertThat(constraintRecord.get("properties").asList()).isEqualTo(List.of("groupId", "artifactId", "version", "classifier"));
        assertThat(constraintRecord.get("ownedIndex").asString()).isEqualTo("unique_gav");
        assertThat(constraintRecord.get("propertyType").asObject()).isNull();
    }

    @Test
    void shouldHandleMultipleInitializations() {
        DatabaseInitializer.initialize(driver);
        EagerResult indexesResult = driver.executableQuery("SHOW INDEXES").execute();
        int previousSize = indexesResult.records().size();

        DatabaseInitializer.initialize(driver);
        assertThat(indexesResult.records()).hasSize(previousSize);
    }

    private void assertIndex(EagerResult result, List<String> properties) {
        assertThat(result.records()).hasSize(1);
        Record record = result.records().getFirst();

        assertThat(record.get("state").asString()).isEqualTo("ONLINE");
        assertThat(record.get("populationPercent").asDouble()).isEqualTo(100.0);
        assertThat(record.get("type").asString()).isEqualTo("RANGE");
        assertThat(record.get("labelsOrTypes").asList()).isEqualTo(List.of("Artifact"));
        assertThat(record.get("properties").asList()).isEqualTo(properties);
        assertThat(record.get("indexProvider").asString()).isEqualTo("range-1.0");
    }
}