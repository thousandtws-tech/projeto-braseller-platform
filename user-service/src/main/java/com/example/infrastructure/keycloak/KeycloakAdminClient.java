package com.example.infrastructure.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP para o Keycloak Admin API.
 *
 * Responsável por criar usuários de contador diretamente no Keycloak,
 * garantindo que possam autenticar via SSO.
 *
 * Endpoints utilizados:
 *   POST {url}/realms/master/protocol/openid-connect/token   → obtém token admin
 *   POST {url}/admin/realms/{realm}/users                     → cria usuário
 *
 * Configuração via variáveis de ambiente:
 *   KEYCLOAK_ADMIN_URL       → base URL do Keycloak
 *   KEYCLOAK_ADMIN_REALM     → realm onde os usuários serão criados (padrão: brasaller)
 *   KEYCLOAK_ADMIN_USERNAME  → usuário administrador (master realm)
 *   KEYCLOAK_ADMIN_PASSWORD  → senha do administrador
 *
 * Se KEYCLOAK_ADMIN_USERNAME ou KEYCLOAK_ADMIN_PASSWORD estiverem vazios,
 * a integração é desativada e o usuário é criado apenas localmente (INVITED).
 */
@ApplicationScoped
public class KeycloakAdminClient {

    private static final Logger LOG = Logger.getLogger(KeycloakAdminClient.class);

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "user.keycloak.admin.url", defaultValue = "")
    String keycloakUrl;

    @ConfigProperty(name = "user.keycloak.admin.realm", defaultValue = "brasaller")
    String realm;

    @ConfigProperty(name = "user.keycloak.admin.username", defaultValue = "")
    String adminUsername;

    @ConfigProperty(name = "user.keycloak.admin.password", defaultValue = "")
    String adminPassword;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Cria o usuário do contador no Keycloak e retorna o Keycloak user ID (subject).
     *
     * @param userId          ID pré-gerado do user-service (salvo como atributo user_id no Keycloak)
     * @param tenantId        ID do tenant (salvo como atributo tenant_id no Keycloak)
     * @param email           E-mail do contador
     * @param firstName       Primeiro nome (obrigatório pelo realm brasaller)
     * @param lastName        Sobrenome (obrigatório pelo realm brasaller)
     * @param temporaryPassword Senha temporária — o Keycloak exigirá troca no primeiro login
     * @return Keycloak user ID se a integração estiver configurada, Optional.empty() caso contrário
     */
    public Optional<String> createAccountantUser(
            String userId,
            String tenantId,
            String email,
            String firstName,
            String lastName,
            String temporaryPassword
    ) {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            LOG.warn("Keycloak admin credentials not configured. Accountant will be created locally only (INVITED status).");
            return Optional.empty();
        }

        try {
            String adminToken = obtainAdminToken();
            String keycloakSubject = createKeycloakUser(adminToken, userId, tenantId, email, firstName, lastName, temporaryPassword);
            LOG.infof("Keycloak user created for accountant %s — subject: %s", email, keycloakSubject);
            return Optional.of(keycloakSubject);
        } catch (Exception e) {
            throw new KeycloakIntegrationException("Failed to create accountant in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém token de acesso administrativo via resource owner password (admin-cli no master realm).
     */
    private String obtainAdminToken() throws Exception {
        String formBody = "grant_type=password"
                + "&client_id=admin-cli"
                + "&username=" + URLEncoder.encode(adminUsername, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(adminPassword, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakUrl + "/realms/master/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Keycloak admin token request failed with status " + response.statusCode()
                    + ": " + response.body());
        }

        JsonNode body = objectMapper.readTree(response.body());
        return body.get("access_token").asText();
    }

    /**
     * Cria o usuário no Keycloak com firstName, lastName, email, senha temporária e atributos do tenant.
     * Retorna o Keycloak user ID extraído do header Location da resposta 201.
     */
    private String createKeycloakUser(
            String adminToken,
            String userId,
            String tenantId,
            String email,
            String firstName,
            String lastName,
            String temporaryPassword
    ) throws Exception {
        Map<String, Object> userPayload = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", true,
                "attributes", Map.of(
                        "tenant_id", List.of(tenantId),
                        "user_id",   List.of(userId)
                ),
                "credentials", List.of(Map.of(
                        "type",      "password",
                        "value",     temporaryPassword,
                        "temporary", false
                ))
        );

        String requestBody = objectMapper.writeValueAsString(userPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakUrl + "/admin/realms/" + realm + "/users"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 409) {
            throw new RuntimeException("User with email " + email + " already exists in Keycloak.");
        }

        if (response.statusCode() != 201) {
            throw new RuntimeException("Keycloak user creation failed with status " + response.statusCode()
                    + ": " + response.body());
        }

        // Location: {url}/admin/realms/{realm}/users/{keycloakUserId}
        String location = response.headers()
                .firstValue("Location")
                .orElseThrow(() -> new RuntimeException("No Location header in Keycloak response."));

        return location.substring(location.lastIndexOf('/') + 1);
    }
}
