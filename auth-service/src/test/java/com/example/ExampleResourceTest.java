package com.example;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@QuarkusTestResource(UserServiceMockResource.class)
class ExampleResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/auth")
                .then()
                .statusCode(200)
                .body(is("Auth Service is running"));
    }

    @Test
    void registersLogsInRefreshesAndLogsOutWithKeycloak() {
        String email = "auth-" + System.nanoTime() + "@brasaller.test";

        String refreshToken = given()
                .contentType("application/json")
                .body("""
                        {
                          "tenantName": "Tenant Auth Test",
                          "fullName": "Auth Owner",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/auth/register")
                .then()
                .statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("refreshToken", startsWith("kc-refresh-"))
                .body("email", is(email))
                .body("profile.provider", is("KEYCLOAK"))
                .body("profile.email", is(email))
                .body("profile.fullName", is("Auth Owner"))
                .body("profile.subject", notNullValue())
                .body("roles.size()", is(2))
                .extract().path("refreshToken");

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("refreshToken", startsWith("kc-refresh-"))
                .body("email", is(email))
                .body("profile.provider", is("KEYCLOAK"))
                .body("profile.email", is(email))
                .body("profile.fullName", is("Auth Owner"));

        String renewedRefreshToken = given()
                .contentType("application/json")
                .body("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(refreshToken))
                .when().post("/auth/refresh")
                .then()
                .statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("refreshToken", startsWith("kc-refresh-"))
                .body("email", is(email))
                .extract().path("refreshToken");

        given()
                .contentType("application/json")
                .body("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(renewedRefreshToken))
                .when().post("/auth/logout")
                .then()
                .statusCode(200)
                .body("revoked", is(true));
    }

    @Test
    void googleAuthorizeUrlUsesKeycloakBrokerHint() {
        given()
                .when().get("/auth/oauth/google/authorize-url")
                .then()
                .statusCode(200)
                .body("authorizeUrl", containsString("/realms/brasaller/protocol/openid-connect/auth"))
                .body("authorizeUrl", containsString("kc_idp_hint=google"));
    }

    @Test
    void googleCallbackUsesKeycloakBroker() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "code": "google-code",
                          "tenantName": "Tenant Google"
                        }
                        """)
                .when().post("/auth/oauth/google/callback")
                .then()
                .statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("email", is("google-oauth@brasaller.test"))
                .body("profile.provider", is("KEYCLOAK"))
                .body("profile.fullName", is("Google OAuth User"));
    }

}
