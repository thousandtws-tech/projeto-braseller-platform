package com.example.infrastructure.connector.mercadolivre;

import com.example.application.exception.ConnectorValidationException;
import com.example.infrastructure.security.Aes256TokenCipher;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class JdbcMercadoLivreTokenRepository {
    private static final String CONNECTOR_NAME = "mercado-livre";

    @Inject
    AgroalDataSource dataSource;

    @Inject
    Aes256TokenCipher tokenCipher;

    public Optional<MercadoLivreConnectorToken> find(String tenantId) {
        String sql = """
                SELECT tenant_id, seller_id, access_token, refresh_token, expires_at
                FROM marketplace_connector_tokens
                WHERE tenant_id = ? AND connector_name = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId);
            statement.setString(2, CONNECTOR_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new MercadoLivreConnectorToken(
                        resultSet.getString("tenant_id"),
                        resultSet.getString("seller_id"),
                        tokenCipher.decrypt(resultSet.getString("access_token")),
                        tokenCipher.decrypt(resultSet.getString("refresh_token")),
                        resultSet.getTimestamp("expires_at").toInstant()
                ));
            }
        } catch (SQLException exception) {
            throw new ConnectorValidationException("mercado_livre_token_repository_unavailable");
        }
    }

    public void save(MercadoLivreConnectorToken token) {
        try (Connection connection = dataSource.getConnection()) {
            update(connection, token);
        } catch (SQLException exception) {
            throw new ConnectorValidationException("mercado_livre_token_repository_unavailable");
        }
    }

    private void update(Connection connection, MercadoLivreConnectorToken token) throws SQLException {
        String updateSql = """
                UPDATE marketplace_connector_tokens
                SET seller_id = ?, access_token = ?, refresh_token = ?, expires_at = ?, updated_at = ?
                WHERE tenant_id = ? AND connector_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setString(1, token.sellerId());
            statement.setString(2, tokenCipher.encrypt(token.accessToken()));
            statement.setString(3, tokenCipher.encrypt(token.refreshToken()));
            statement.setTimestamp(4, Timestamp.from(token.expiresAt()));
            statement.setTimestamp(5, Timestamp.from(Instant.now()));
            statement.setString(6, token.tenantId());
            statement.setString(7, CONNECTOR_NAME);
            if (statement.executeUpdate() > 0) {
                return;
            }
        }
        insert(connection, token);
    }

    private void insert(Connection connection, MercadoLivreConnectorToken token) throws SQLException {
        String insertSql = """
                INSERT INTO marketplace_connector_tokens
                    (tenant_id, connector_name, seller_id, access_token, refresh_token, expires_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, token.tenantId());
            statement.setString(2, CONNECTOR_NAME);
            statement.setString(3, token.sellerId());
            statement.setString(4, tokenCipher.encrypt(token.accessToken()));
            statement.setString(5, tokenCipher.encrypt(token.refreshToken()));
            statement.setTimestamp(6, Timestamp.from(token.expiresAt()));
            statement.setTimestamp(7, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }
}
