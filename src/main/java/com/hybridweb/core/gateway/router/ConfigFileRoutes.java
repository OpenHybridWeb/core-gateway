package com.hybridweb.core.gateway.router;

import io.quarkus.runtime.ShutdownEvent;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.web.Router;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routes defined from config file
 */
@ApplicationScoped
public class ConfigFileRoutes {

    private static final Logger log = Logger.getLogger(ConfigFileRoutes.class);

    @ConfigProperty(name = "app.gateway.router.configpath")
    Optional<String> configPath;

    @Inject
    Vertx vertx;

    /**
     * Clients for routing. Key is target url and value is the client
     */
    Map<String, HttpClient> clients;

    public void registerServiceRoutes(@Observes Router router) throws FileNotFoundException, MalformedURLException {
        if (configPath.isEmpty()) {
            log.info("No Config Path defined. Routes from config file not registered");
            return;
        }
        Yaml yaml = new Yaml();

        File configFile = new File(configPath.get());
        Map<String, Object> config = yaml.load(new FileInputStream(configFile));

        List<Map<String, String>> routes = (List<Map<String, String>>) config.get("routes");

        clients = new HashMap<>(routes.size());

        for (Map<String, String> route : routes) {
            registerRoute(router, route.get("context"), route.get("url"));
        }

    }

    protected HttpClient getClient(String address) throws MalformedURLException {
        for (Map.Entry<String, HttpClient> entry : clients.entrySet()) {
            if (entry.getKey().equals(address)) {
                return entry.getValue();
            }
        }

        log.infof("Creating new client for %s", address);
        URL url = new URL(address);
        HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                .setDefaultPort(url.getPort())
                .setDefaultHost(url.getHost())
        );
        clients.put(address, client);
        return client;
    }

    protected void registerRoute(Router router, String routeContext, String address) throws MalformedURLException {
        log.infof("Registering router. routeContext=%s url=%s", routeContext, address);
        final HttpClient client = getClient(address);

        router.route(routeContext).handler(sourceContext -> {
            HttpServerRequest sourceRequest = sourceContext.request();
            String sourceUri = sourceRequest.uri();
            HttpClientRequest targetRequest = client.request(sourceRequest.method(), sourceUri);

            targetRequest.handler(targetResponse -> {
                HttpServerResponse sourceResponse = sourceContext.response();
                // copy headers from target response
                sourceResponse.headers().addAll(targetResponse.headers());
                sourceResponse.setStatusCode(targetResponse.statusCode());
                // copy output
                targetResponse.pipeTo(sourceResponse);
            }).exceptionHandler(ex -> log.error("Error on calling target url=" + sourceUri, ex));

            targetRequest.headers().addAll(sourceRequest.headers());
            // copy request body and "fire" the target request when done
            sourceRequest.pipeTo(targetRequest, end -> targetRequest.end());
        }).failureHandler(ctx -> {
            log.error("Error on calling url=" + ctx.request().absoluteURI(), ctx.failure());
            ctx.response().setStatusCode(502).end();
        });
    }

    void closeClients(@Observes ShutdownEvent ev) {
        if (clients == null) {
            return;
        }
        for (Map.Entry<String, HttpClient> entry : clients.entrySet()) {
            log.infof("Closing client for url=%s", entry.getKey());
            entry.getValue().close();
        }
    }

}
