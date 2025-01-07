package dev.harrel.jarhell;

import io.avaje.inject.BeanScope;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

public class App implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            App app = new App();
            app.start(8060);
            Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        } catch (Throwable e) {
            logger.error("Creating bean scope (context) failed", e);
            System.exit(1);
        }
    }

    private BeanScope beanScope;
    private Javalin server;

    public void start(int port) {
        if (this.beanScope != null) {
            throw new IllegalStateException("App already started");
        }

        this.beanScope = BeanScope.builder().build();

        this.server = beanScope.get(Javalin.class);
        server.start(port);
    }

    @Override
    public void close() {
        server.stop();
        beanScope.close();
    }

    public BeanScope getBeanScope() {
        return beanScope;
    }
}
