package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.extension.EnvironmentTest;
import dev.harrel.jarhell.model.Gav;
import io.avaje.config.Config;
import io.avaje.inject.BeanScope;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MavenRunnerTest {
    @Test
    void name() {
        Config.setProperty("maven.repo-url", "https://repo.maven.apache.org/maven2");
        BeanScope beanScope = BeanScope.builder()
                .bean(Driver.class, mock(Driver.class))
                .build();
        MavenRunner mavenRunner = beanScope.get(MavenRunner.class);

//        DependencyNode node = mavenRunner.collectDependencies(new Gav("run.mone", "nacos-client", "1.2.1-mone-v3"));

//        DependencyNode node = mavenRunner.collectDependencies(
//                new Gav("org.apache.hadoop", "hadoop-mapreduce-client-core", "2.6.5"));
//        DependencyNode node = mavenRunner.collectDependencies(new Gav("dev.harrel", "json-schema", "1.7.1"));
        System.out.println(node);
    }
}