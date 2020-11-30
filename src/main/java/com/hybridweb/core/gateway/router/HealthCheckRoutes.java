package com.hybridweb.core.gateway.router;

import io.vertx.ext.web.Router;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

/**
 * Health checks routes
 */
@ApplicationScoped
public class HealthCheckRoutes {

    private static final Logger log = Logger.getLogger(HealthCheckRoutes.class);

    public void registerHealthChecks(@Observes Router router) {
        log.info("Registering Health API routes under /_api/health");
        router.get("/_api/health/live").handler(rc -> rc.response().end("live"));
        router.get("/_api/health/ready").handler(rc -> rc.response().end("ready"));
    }

}
