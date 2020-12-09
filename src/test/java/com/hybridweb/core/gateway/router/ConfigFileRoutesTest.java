package com.hybridweb.core.gateway.router;

import com.hybridweb.core.gateway.router.mock.VertxMockServer;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Test of {@link ConfigFileRoutes}
 */
@QuarkusTest
public class ConfigFileRoutesTest {

    static Vertx vertx;

    @BeforeAll
    public static void before() {
        vertx = Vertx.vertx();

        VertxMockServer staticMockServer = new VertxMockServer("staticMockServer", 8760, 0, true);
        staticMockServer.getRouter().get("/theme/css/test.css").handler(ctx -> {
            ctx.response().setStatusCode(200).end("test css");
        });
        staticMockServer.getRouter().get("/spa1/index.html").handler(ctx -> {
            ctx.response().setStatusCode(200).end("<html></html>");
        });
        staticMockServer.getRouter().get("/_root/index.html").handler(ctx -> {
            ctx.response().setStatusCode(200).end("<html><title>homepage</title></html>");
        });
        vertx.deployVerticle(staticMockServer);

        VertxMockServer apiMockServer = new VertxMockServer("apiMockServer", 8770, 0, true);

        apiMockServer.getRouter().post("/api/search").handler(ctx -> {
            ctx.response().setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end("{ result: \"" + ctx.getBodyAsString() + "\" }");
        });
        apiMockServer.getRouter().post("/api/headers").handler(ctx -> {
            ctx.response().setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end("{ reqheader: \"" + ctx.request().getHeader("req-test-header") + "\" }");
        });
        vertx.deployVerticle(apiMockServer);
    }

    @AfterAll
    public static void after() {
        vertx.close();
    }


    @Test
    public void testTheme() {
        given().when().get("/theme/css/test.css")
                .then().statusCode(200)
                .body(is("test css"));
    }

    @Test
    public void testThemeNotFound() {
        given().when().get("/theme/NOTFOUND")
                .then().statusCode(404);
    }

    @Test
    public void testSpa() {
        given().when().get("/test-spa1/index.html")
                .then().statusCode(200)
                .body(is("<html></html>"));
    }

    @Test
    public void testRoot() {
        given().when().get("/index.html")
                .then().statusCode(200)
                .body(is("<html><title>homepage</title></html>"));
    }

    @Test
    public void testApi() {
        given().body("test-body").when().post("/api/search")
                .then().statusCode(200)
                .header("content-type", is("application/json"))
                .body(is("{ result: \"test-body\" }"));
    }

    @Test
    public void testApiNoBody() {
        given().when().post("/api/search")
                .then().statusCode(200)
                .header("content-type", is("application/json"))
                .body(is("{ result: \"\" }"));
    }
    @Test
    public void testApiReqHeader() {
        given().when().header(new Header("req-test-header", "req-test-header-value")).post("/api/headers")
                .then().statusCode(200)
                .header("content-type", is("application/json"))
                .body(is("{ reqheader: \"req-test-header-value\" }"));
    }
}
