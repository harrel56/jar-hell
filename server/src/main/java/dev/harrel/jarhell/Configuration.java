package dev.harrel.jarhell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import org.neo4j.driver.*;

import java.net.URI;
import java.util.Map;

@Factory
class Configuration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new JavaTimeModule());
    }

    @Bean
    Driver neo4jDriver() {
        System.out.println(io.avaje.config.Config.getURI("neo4j.uri"));
        Map<String, String> env = System.getenv();
        URI dbUri = URI.create(env.get("neo4j.uri"));
        AuthToken authToken = AuthTokens.basic(env.get("neo4j.username"), env.get("neo4j.password"));
        Config config = Config.builder().withLogging(Logging.slf4j()).build();
        return GraphDatabase.driver(dbUri, authToken, config);
    }
}
