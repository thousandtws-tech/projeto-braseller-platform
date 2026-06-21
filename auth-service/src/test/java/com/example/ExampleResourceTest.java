package com.example;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
    void registersRequiresEmailVerificationThenLogsInRefreshesAndLogsOutWithKeycloak() {
        String email = "auth-" + System.nanoTime() + "@brasaller.test";

        given()
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
                .statusCode(202)
                .body("email", is(email))
                .body("status", is("PENDING_EMAIL_VERIFICATION"));

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
                .statusCode(401)
                .body("message", is("email_not_verified"));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "code": "123456"
                        }
                        """.formatted(email))
                .when().post("/auth/email-verification/verify")
                .then()
                .statusCode(200)
                .body("message", is("email_verified"));

        String refreshToken = given()
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
                .body("profile.fullName", is("Auth Owner"))
                .extract().path("refreshToken");

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
    void passwordResetUsesSingleUseCode() {
        String email = "reset-" + System.nanoTime() + "@brasaller.test";

        given()
                .contentType("application/json")
                .body("""
                        {
                          "tenantName": "Tenant Reset Test",
                          "fullName": "Reset Owner",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/auth/register")
                .then()
                .statusCode(202);

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "code": "123456"
                        }
                        """.formatted(email))
                .when().post("/auth/email-verification/verify")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s"
                        }
                        """.formatted(email))
                .when().post("/auth/password-reset/request")
                .then()
                .statusCode(202)
                .body("message", is("Se o e-mail existir, enviaremos instrucoes para redefinir a senha."));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "code": "123456"
                        }
                        """.formatted(email))
                .when().post("/auth/password-reset/validate")
                .then()
                .statusCode(200)
                .body("valid", is(true));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "code": "123456",
                          "newPassword": "NewChange123!"
                        }
                        """.formatted(email))
                .when().post("/auth/password-reset/reset")
                .then()
                .statusCode(200)
                .body("message", is("password_reset"));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "code": "123456",
                          "newPassword": "Another123!"
                        }
                        """.formatted(email))
                .when().post("/auth/password-reset/reset")
                .then()
                .statusCode(400)
                .body("message", is("invalid_or_expired_code"));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "NewChange123!"
                        }
                        """.formatted(email))
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("email", is(email));
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
