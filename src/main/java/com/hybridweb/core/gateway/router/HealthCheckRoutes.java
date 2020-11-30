package com.hybridweb.core.gateway.router;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.core.http.HttpMethod;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Health checks routes
 */
@RouteBase(path = "/_api/health", produces = "text/plain")
public class HealthCheckRoutes {

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

    @Route(path = "", methods = HttpMethod.GET, produces = "application/json")
    Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("clients", configFileRoutes.getClients().keySet());
        return info;
    }

}
