package dev.harrel.jarhell;

import io.avaje.inject.BeanScope;
import io.avaje.jex.Jex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

public class App implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    static void main() {
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
    private Jex.Server server;

    public void start(int port) {
        if (this.beanScope != null) {
            throw new IllegalStateException("App already started");
        }

        this.beanScope = BeanScope.builder().build();

        this.server = beanScope.get(Jex.class).port(port).start();
    }

    @Override
    public void close() {
        server.shutdown();
        beanScope.close();
    }

    public BeanScope getBeanScope() {
        return beanScope;
    }
}
