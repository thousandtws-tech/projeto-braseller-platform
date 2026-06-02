package com.example.infrastructure.persistence;

import com.example.application.port.out.ClicksignWebhookEventRepository;
import com.example.domain.model.ClicksignWebhookEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

@ApplicationScoped
public class JdbcClicksignWebhookEventRepository implements ClicksignWebhookEventRepository {
    @Inject
    DataSource dataSource;

    @Override
    public ClicksignWebhookEvent save(ClicksignWebhookEvent event, String payloadJson, String contentHmac) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO clicksign_webhook_events
                     (id, event_name, account_key, envelope_id, document_key, event_occurred_at,
                      payload_json, content_hmac, processing_status, processing_message, received_at, processed_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, event.id());
            statement.setString(2, event.eventName());
            statement.setString(3, event.accountKey());
            statement.setString(4, event.envelopeId());
            statement.setString(5, event.documentKey());
            statement.setTimestamp(6, event.occurredAt() == null ? null : Timestamp.from(event.occurredAt()));
            statement.setString(7, payloadJson);
            statement.setString(8, contentHmac);
            statement.setString(9, event.processingStatus());
            statement.setString(10, event.processingMessage());
            statement.setTimestamp(11, Timestamp.from(event.receivedAt()));
            statement.setTimestamp(12, Timestamp.from(event.processedAt()));
            statement.executeUpdate();
            return find(event.id());
        } catch (Exception exception) {
            throw new RepositoryException("Could not save Clicksign webhook event", exception);
        }
    }

    private ClicksignWebhookEvent find(String id) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, event_name, account_key, envelope_id, document_key, event_occurred_at,
                            processing_status, processing_message, received_at, processed_at
                     FROM clicksign_webhook_events
                     WHERE id = ?
                     """)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new ClicksignWebhookEvent(
                            resultSet.getString("id"),
                            resultSet.getString("event_name"),
                            resultSet.getString("account_key"),
                            resultSet.getString("envelope_id"),
                            resultSet.getString("document_key"),
                            resultSet.getTimestamp("event_occurred_at") == null ? null : resultSet.getTimestamp("event_occurred_at").toInstant(),
                            resultSet.getString("processing_status"),
                            resultSet.getString("processing_message"),
                            resultSet.getTimestamp("received_at").toInstant(),
                            resultSet.getTimestamp("processed_at") == null ? null : resultSet.getTimestamp("processed_at").toInstant()
                    );
                }
            }
        }
        throw new RepositoryException("Could not find saved Clicksign webhook event", null);
    }
}
