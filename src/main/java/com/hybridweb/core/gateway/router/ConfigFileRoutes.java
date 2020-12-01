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
    Map<String, HttpClient> clients = new HashMap<>();

    public void registerServiceRoutes(@Observes Router router) throws FileNotFoundException, MalformedURLException {
        if (configPath.isEmpty()) {
            log.info("No Config Path defined. Routes from config file not registered");
            return;
        }
        Yaml yaml = new Yaml();

        File configFile = new File(configPath.get());
        Map<String, Object> config = yaml.load(new FileInputStream(configFile));

        List<Map<String, String>> routes = (List<Map<String, String>>) config.get("routes");

        for (Map<String, String> route : routes) {
            registerRoute(router, route.get("context"), route.get("url"));
        }
    }

    /**
     * Get client resp. register new one
     *
     * @param address
     * @return http client for gateway
     * @throws MalformedURLException
     */
    protected HttpClient getClient(String address) throws MalformedURLException {
        for (Map.Entry<String, HttpClient> entry : clients.entrySet()) {
            if (entry.getKey().equals(address)) {
                return entry.getValue();
            }
        }

        URL url = new URL(address);
        int port = -1;
        if (url.getPort() != -1) {
            port = url.getPort();
        } else {
            if (address.startsWith("http")) {
                port = 80;
            } else if (address.startsWith("https")) {
                port = 443;
            }
        }
        log.infof("Creating new client for host=%s port=%s", url.getHost(), port);

        // TODO: Add possibility to pass client defaults via JSON/YAML
        HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                .setDefaultPort(port)
                .setDefaultHost(url.getHost())
        );
        clients.put(address, client);
        return client;
    }

    protected void registerRoute(Router router, String routeContext, String address) throws MalformedURLException {
        log.infof("Registering route. routeContext=%s url=%s", routeContext, address);
        final HttpClient client = getClient(address);

        router.route(routeContext).handler(sourceContext -> {
            HttpServerRequest sourceRequest = sourceContext.request();
            String sourceUri = sourceRequest.uri();

            // Create target request template
            HttpClientRequest targetRequest = client.request(sourceRequest.method(), sourceUri);
            // copy headers from source to target
            targetRequest.headers().addAll(sourceRequest.headers());
            // register the response and exception handler
            targetRequest.handler(targetResponse -> copyTargetResponseToSource(targetResponse, sourceContext.response()))
                    .exceptionHandler(ex -> log.error("Error on calling target url=" + sourceUri, ex));

            // copy request body and "fire" the target request when copying is done
            sourceRequest.pipeTo(targetRequest, end -> targetRequest.end());

        }).failureHandler(sourceContext -> {
            log.error("Error on calling url=" + sourceContext.request().absoluteURI(), sourceContext.failure());
            sourceContext.response().setStatusCode(502).end();
        });
    }

    protected void copyTargetResponseToSource(HttpClientResponse targetResponse, HttpServerResponse sourceResponse) {
        // copy headers from target response
        sourceResponse.headers().addAll(targetResponse.headers());
        // copy status code
        sourceResponse.setStatusCode(targetResponse.statusCode());
        // copy target output back to source response
        targetResponse.pipeTo(sourceResponse);
    }

    public Map<String, HttpClient> getClients() {
        return clients;
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
