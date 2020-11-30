package com.hybridweb.core.gateway.router.mock;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.jboss.logging.Logger;

public class VertxMockServer extends AbstractVerticle {

    private Logger log;

    protected int port;
    protected long serviceDelay;
    protected boolean logEnabled;

    protected int connectionsCount = 0;

    protected RequestCountHandler countHandler = RequestCountHandler.create();

    private Router router;

    public VertxMockServer(String name, int port, long serviceDelay, boolean logEnabled) {
        log = Logger.getLogger(VertxMockServer.class + "[" + name + "]");

        this.port = port;
        this.serviceDelay = serviceDelay;
        this.logEnabled = logEnabled;

        router = createRouter();
    }

    protected Router createRouter() {
        Router router = Router.router(vertx);

        Route defaultRoute = router.route();

        defaultRoute.handler(BodyHandler.create(false));

        if (logEnabled) {
            defaultRoute.handler(BodyHandler.create());
            defaultRoute.handler(LoggingHandler.create()
                    .printBody(true));
        }

        if (serviceDelay > 0) {
            defaultRoute.handler(DelayHandler.create()
                    .delay(serviceDelay));
        }

        defaultRoute.handler(countHandler);

        return router;
    }


    public void start() {
        vertx.createHttpServer()
                .connectionHandler(c -> connectionsCount++)
                .exceptionHandler(t -> log.error("Error", t))
                .requestHandler(router)
                .listen(port);

        vertx.setPeriodic(10000, e -> log.info(this.info()));

        log.infof("MockServer started. logEnabled=%s, dyFoPort=%s, serviceDelay=%s", logEnabled, port, serviceDelay);
    }

    public String info() {
        return String.format("connectionsCount=%s, reqCount=%s", connectionsCount, getCount());
    }

    public long getCount() {
        return countHandler.getCount();
    }

    public Router getRouter() {
        return router;
    }
}
