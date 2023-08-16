package dev.harrel.jarhell;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.supplier.ConsoleRepositoryListener;
import org.eclipse.aether.supplier.ConsoleTransferListener;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

import java.util.function.Consumer;

public class Main {

    public static void main(String[] arg) {


        Processor processor = new Processor();

        Consumer<JavalinConfig> configConsumer = config -> {
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)));
        };

        Javalin.create(configConsumer)
                .get("/analyze", new AnalyzeHandler(processor))
                .start(8060);
    }
}
