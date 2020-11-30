package com.hybridweb.core.gateway.router.mock;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

import java.io.PrintStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Simple logging routing handler writing to defined printStream.
 * Can be registered e.g.
 * <pre>
 * router.route().handler(LoggingRoutingHandler.create());
 * </pre>
 */
public class LoggingHandler implements Handler<RoutingContext> {

    PrintStream printStream = System.out;
    boolean printBody = false;

    public static LoggingHandler create() {
        return new LoggingHandler();
    }

    public LoggingHandler printStream(PrintStream printStream) {
        this.printStream = printStream;
        return this;
    }

    /**
     * Print body to log. The body is obtained from {ctx.getBodyAsString()} so make sure that BodyHandler is registered via
     * router.route().handler(BodyHandler.create());
     *
     * @param printBody
     * @return
     */
    public LoggingHandler printBody(boolean printBody) {
        this.printBody = printBody;
        return this;
    }

    @Override
    public void handle(RoutingContext ctx) {
        long start = System.nanoTime();
        ctx.addHeadersEndHandler(v -> {
            long duration = MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS);
            HttpServerRequest req = ctx.request();
            printStream.print(String.format("[%4s] %s - %sms", req.method(), req.uri(), duration));
            if (printBody) {
                printStream.print(" - request body: " + ctx.getBodyAsString());
            }
            printStream.println();
        });
        ctx.next();
    }

}
