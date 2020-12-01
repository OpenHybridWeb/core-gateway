package com.hybridweb.core.gateway.router;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.core.http.HttpMethod;
import org.jboss.logging.Logger;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Health checks routes
 */
@RouteBase(path = "/_api/health", produces = "text/plain")
public class HealthCheckRoutes {

    private static final Logger log = Logger.getLogger(HealthCheckRoutes.class);

    @Inject
    ConfigFileRoutes configFileRoutes;

    @Route(path = "live", methods = HttpMethod.GET)
    String live() {
        return "live";
    }

    @Route(path = "ready", methods = HttpMethod.GET)
    String ready() {
        // consider analysis if gateway works OK.
        return "ready";
    }

    @Route(path = "info", methods = HttpMethod.GET, produces = "application/json")
    Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("clients", configFileRoutes.getClients().keySet());
        return info;
    }

    void onStart(@Observes StartupEvent ev) {
        log.info("Health checks registered. health_check_info: http://localhost:8080/_api/health/info");
    }


}
