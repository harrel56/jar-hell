package dev.harrel.jarhell;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import dev.harrel.jarhell.error.ErrorResponse;
import dev.harrel.jarhell.error.ResourceNotFoundException;
import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.GenericType;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.plugin.Plugin;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] arg) {
        BeanScope beanScope = BeanScope.builder().build();

        Consumer<JavalinConfig> configConsumer = beanScope.get(new GenericType<Consumer<JavalinConfig>>() {}, "javalinConfig");
        Javalin server = Javalin.create(configConsumer)
                .exception(ResourceNotFoundException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.NOT_FOUND);
                })
                .exception(ValueInstantiationException.class, (e, ctx) -> {
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), ExceptionUtils.getRootCause(e).getMessage()));
                    ctx.status(HttpStatus.BAD_REQUEST);
                })
                .exception(Exception.class, (e, ctx) -> {
                    logger.error("Unhandled exception occurred", e);
                    ctx.json(new ErrorResponse(ctx.fullUrl(), ctx.method(), e.getMessage()));
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                });

        List<Plugin> plugins = beanScope.list(Plugin.class); // web routes
        plugins.forEach(plugin -> plugin.apply(server));
        server.start(8060);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
            beanScope.close();
        }));
    }
}
