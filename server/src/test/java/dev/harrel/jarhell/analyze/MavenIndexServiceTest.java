package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.Gav;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MavenIndexServiceTest {
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "dev.harrel|json-schema|1.9.0|NA; pom.sha512|1763742557000|128|1|1|0|pom.sha512; dev.harrel:json-schema:1.9.0",
            "zw.co.paynow|java-sdk|1.1.2|NA; pom.sha512|1738155098000|128|1|1|0|pom.sha512; zw.co.paynow:java-sdk:1.1.2",
            "zw.co.paynow|java-sdk|1.1.1|NA; jar|1560505847000|25598|1|1|1|jar; zw.co.paynow:java-sdk:1.1.1",
            "zone.src.sheaf|sheaf-parent|1.8|NA; pom|1513079077000|23840|0|0|1|pom; zone.src.sheaf:sheaf-parent:1.8",
            "zone.gryphon.maven.plugins|scm-metadata-maven-plugin|1.8|NA; maven-plugin|1573545015000|36901|1|1|1|jar; zone.gryphon.maven.plugins:scm-metadata-maven-plugin:1.8",
            "zone.dragon.dropwizard|dropwizard-kotlin|1.0.0|tests|jar; jar|1734558020000|34635|2|2|1|jar; zone.dragon.dropwizard:dropwizard-kotlin:1.0.0:tests",
            "zone.dragon.protobuf|protoc-gen-openapi|1.0.7|windows-x86_64|exe; exe|1732226036000|14951936|2|2|1|exe; zone.dragon.protobuf:protoc-gen-openapi:1.0.7:windows-x86_64",
            "zone.dragon.baharclerode|cassandra-unit|4.3.2.3|bin|tar.gz; tar.gz|1632959066000|62854614|2|2|1|tar.gz; zone.dragon.baharclerode:cassandra-unit:4.3.2.3:bin",
    })
    void mapsRowToGav(String uinfo, String info, String expected) {
        Gav gav = MavenIndexService.rowToGav(Map.of("u", uinfo, "i", info));

        assertThat(gav).isEqualTo(Gav.fromCoordinate(expected).orElseThrow());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "dev.harrel|json-schema|1.9.0|sources|jar; jar|1763742557000|73411|2|2|1|jar",
            "zw.co.paynow|java-sdk|1.1.2|javadoc|jar; jar|1738155111000|164136|2|2|1|jar",
            "zw.co.paynow|java-sdk|1.1.2|sources|jar.sha512; jar.sha512|1738155110000|128|2|2|0|jar.sha512",
            "zone.cogni.semanticz|semanticz-shaclviz|1.0.2|executable|jar.sha512; jar.sha512|1736849850000|128|2|2|0|jar.sha512",
            "zone.cogni.semanticz|semanticz-shaclviz|1.0.2|executable|jar.sha256; jar.sha256|1736849850000|64|2|2|0|jar.sha256",
            "zone.cogni.semanticz|semanticz-shaclviz|1.0.2|executable|jar.asc.sha512; jar.asc.sha512|1736849848000|128|2|2|0|jar.asc.sha512",
            "zone.cogni.semanticz|semanticz-shaclviz|1.0.2|executable|jar.asc; jar.asc|1736849848000|488|2|2|0|jar.asc",
    })
    void skipsClassifiersAndChecksums(String uinfo, String info) {
        Gav gav = MavenIndexService.rowToGav(Map.of("u", uinfo, "i", info));

        assertThat(gav).isNull();
    }

    @Test
    void skipsRowWithoutUinfo() {
        assertThat(MavenIndexService.rowToGav(Map.of("desc", "NexusIndex", "IDXINFO", "1.0|central"))).isNull();
    }

    @Test
    void skipsGroupRow() {
        assertThat(MavenIndexService.rowToGav(Map.of("u", "zone.dragon|protobuf"))).isNull();
    }
}
