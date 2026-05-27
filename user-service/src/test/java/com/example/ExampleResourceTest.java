package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ExampleResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/users")
                .then()
                .statusCode(200)
                .body(is("User Service is running"));
    }

    @Test
    void registersTenantWithAdminUser() {
        String email = "owner-" + System.nanoTime() + "@brasaller.test";

        given()
                .contentType("application/json")
                .body("""
                        {
                          "legalName": "Brasaller Test LTDA",
                          "tradeName": "Brasaller Test",
                          "adminName": "Owner Test",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/tenants/register")
                .then()
                .statusCode(201)
                .body("tenant.status", is("ACTIVE"))
                .body("adminUser.email", is(email))
                .body("adminUser.fullName", is("Owner Test"))
                .body("adminUser.preferredUsername", is(email))
                .body("adminUser.emailVerified", is(true))
                .body("adminUser.provider", is("PASSWORD"))
                .body("adminUser.roles.size()", is(2));
    }

    @Test
    void verifiesPasswordOnlyWithInternalToken() {
        String email = "verify-" + System.nanoTime() + "@brasaller.test";

        given()
                .contentType("application/json")
                .body("""
                        {
                          "legalName": "Brasaller Verify LTDA",
                          "tradeName": "Brasaller Verify",
                          "adminName": "Verify Owner",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/tenants/register")
                .then()
                .statusCode(201);

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/internal/identity/verify-password")
                .then()
                .statusCode(403)
                .body("message", is("invalid_internal_token"));

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/internal/identity/verify-password")
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("fullName", is("Verify Owner"))
                .body("preferredUsername", is(email))
                .body("emailVerified", is(true))
                .body("provider", is("PASSWORD"))
                .body("roles.size()", is(2));
    }

    @Test
    void syncsExternalProfileWithInternalToken() {
        String email = "profile-" + System.nanoTime() + "@brasaller.test";

        given()
                .contentType("application/json")
                .body("""
                        {
                          "legalName": "Brasaller Profile LTDA",
                          "tradeName": "Brasaller Profile",
                          "adminName": "Initial Owner",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/tenants/register")
                .then()
                .statusCode(201);

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "provider": "KEYCLOAK",
                          "providerSubject": "keycloak-subject-123",
                          "fullName": "Google Profile Owner",
                          "preferredUsername": "google.owner",
                          "firstName": "Google",
                          "lastName": "Owner",
                          "pictureUrl": "https://example.com/avatar.png",
                          "emailVerified": true
                        }
                        """.formatted(email))
                .when().post("/users/internal/identity/sync-profile")
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("fullName", is("Google Profile Owner"))
                .body("preferredUsername", is("google.owner"))
                .body("firstName", is("Google"))
                .body("lastName", is("Owner"))
                .body("pictureUrl", is("https://example.com/avatar.png"))
                .body("provider", is("KEYCLOAK"))
                .body("providerSubject", is("keycloak-subject-123"));

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/internal/identity/verify-password")
                .then()
                .statusCode(200)
                .body("fullName", is("Google Profile Owner"))
                .body("preferredUsername", is("google.owner"))
                .body("firstName", is("Google"))
                .body("lastName", is("Owner"))
                .body("pictureUrl", is("https://example.com/avatar.png"))
                .body("provider", is("KEYCLOAK"))
                .body("providerSubject", is("keycloak-subject-123"));
    }

    @Test
    void adminCanCreateAccountantAndListMembersWithJwt() {
        String ownerEmail = "owner-accountant-" + System.nanoTime() + "@brasaller.test";
        var registration = given()
                .contentType("application/json")
                .body("""
                        {
                          "legalName": "Brasaller Accountant LTDA",
                          "tradeName": "Brasaller Accountant",
                          "adminName": "Owner Accountant",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(ownerEmail))
                .when().post("/users/tenants/register")
                .then()
                .statusCode(201)
                .extract();

        String tenantId = registration.path("tenant.id");
        String adminUserId = registration.path("adminUser.id");
        String token = token(tenantId, adminUserId, ownerEmail, "ADMIN", "VENDEDOR");
        String accountantEmail = "contador-" + System.nanoTime() + "@brasaller.test";

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "fullName": "Contador Teste",
                          "temporaryPassword": "ChangeMe123!"
                        }
                        """.formatted(accountantEmail))
                .when().post("/users/tenants/%s/accountants".formatted(tenantId))
                .then()
                .statusCode(201)
                .body("tenantId", is(tenantId))
                .body("email", is(accountantEmail))
                .body("readOnly", is(true));

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/users/tenants/%s/members".formatted(tenantId))
                .then()
                .statusCode(200)
                .body("email", hasItem(ownerEmail))
                .body("email", hasItem(accountantEmail));
    }

    @Test
    void protectedTenantEndpointsRequireJwt() {
        given()
                .when().get("/users/tenants/tenant-a/members")
                .then()
                .statusCode(401)
                .body("message", is("missing_bearer_token"));
    }

    @Test
    void rejectsCrossTenantMemberList() {
        given()
                .header("Authorization", "Bearer " + token("tenant-b", "user-b", "seller-b@brasaller.test", "ADMIN"))
                .when().get("/users/tenants/tenant-a/members")
                .then()
                .statusCode(403)
                .body("message", is("tenant_mismatch"));
    }

    @Test
    void accountantCannotGrantAccountantAccess() {
        String ownerEmail = "owner-readonly-" + System.nanoTime() + "@brasaller.test";
        var registration = given()
                .contentType("application/json")
                .body("""
                        {
                          "legalName": "Brasaller Readonly LTDA",
                          "tradeName": "Brasaller Readonly",
                          "adminName": "Readonly Owner",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(ownerEmail))
                .when().post("/users/tenants/register")
                .then()
                .statusCode(201)
                .extract();

        String tenantId = registration.path("tenant.id");
        String token = token(tenantId, "accountant-user", "contador@brasaller.test", "CONTADOR");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "email": "blocked-contador@brasaller.test",
                          "fullName": "Blocked Contador",
                          "temporaryPassword": "ChangeMe123!"
                        }
                        """)
                .when().post("/users/tenants/%s/accountants".formatted(tenantId))
                .then()
                .statusCode(403)
                .body("message", is("admin_role_required"));
    }

    private String token(String tenantId, String userId, String email, String... roles) {
        String header = encode("""
                {"alg":"HS256","typ":"JWT"}
                """);
        long expiration = Instant.now().plusSeconds(300).getEpochSecond();
        String groups = Arrays.stream(roles)
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(", "));
        String payload = encode("""
                {
                  "iss": "brasaller-auth",
                  "aud": "brasaller-platform",
                  "exp": %d,
                  "tenant_id": "%s",
                  "user_id": "%s",
                  "email": "%s",
                  "groups": [%s]
                }
                """.formatted(expiration, tenantId, userId, email, groups));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("dev-only-change-me-please-32-bytes-minimum".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

}
