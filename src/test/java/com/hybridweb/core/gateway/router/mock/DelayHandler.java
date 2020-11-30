package com.hybridweb.core.gateway.router.mock;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler pause request for defined delay
 * <pre>
 * router.route().handler(DelayRoutingHandler.create().delay(delay));
 * </pre>
 */
public class DelayHandler implements Handler<RoutingContext> {

    long delay;

    public static DelayHandler create() {
        return new DelayHandler();
    }

    public DelayHandler delay(long delay) {
        this.delay = delay;
        return this;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if (delay > 0) {
            var request = routingContext.request();
            // Pause the request
            request.pause();
            routingContext.vertx().setTimer(delay, i -> {
                // Resume the request
                request.resume();
                routingContext.next();
            });
        } else {
            routingContext.next();
        }
    }

}
