package com.hybridweb.core.gateway.router;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class HealthCheckTest {

    @Test
    public void testLiveCheck() {
        given()
                .when().get("/_api/health/live")
                .then()
                .statusCode(200)
                .body(is("live"));
    }

    @Test
    public void testReadyCheck() {
        given()
                .when().get("/_api/health/ready")
                .then()
                .statusCode(200)
                .body(is("ready"));
    }

}
