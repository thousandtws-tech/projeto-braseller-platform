package com.example.infrastructure.persistence;

import com.example.application.port.out.NotificationRepository;
import com.example.domain.model.DeliveryStatus;
import com.example.domain.model.NotificationChannel;
import com.example.domain.model.NotificationMessage;
import com.example.domain.model.NotificationPreference;
import com.example.domain.model.NotificationStatus;
import com.example.domain.model.NotificationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcNotificationRepository implements NotificationRepository {
    @Inject
    DataSource dataSource;

    @Override
    public NotificationPreference getPreference(String tenantId) {
        return findPreference(tenantId).orElseGet(() -> savePreference(NotificationPreference.defaults(tenantId)));
    }

    @Override
    public NotificationPreference savePreference(NotificationPreference preference) {
        try (Connection connection = dataSource.getConnection()) {
            if (updatePreference(connection, preference) == 0) {
                insertPreference(connection, preference);
            }
            return getPreferenceRow(preference.tenantId()).orElse(preference);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save notification preference", exception);
        }
    }

    @Override
    public List<NotificationPreference> listMonthlyClosingPreferences() {
        return listPreferences("""
                SELECT tenant_id, email_enabled, new_sale_enabled, monthly_closing_enabled,
                       ml_payment_release_enabled, weekly_accountant_report_enabled,
                       recipient_email, accountant_email, updated_at
                FROM notification_preferences
                WHERE monthly_closing_enabled = TRUE
                  AND email_enabled = TRUE
                  AND recipient_email IS NOT NULL
                ORDER BY tenant_id
                """);
    }

    @Override
    public List<NotificationPreference> listMlPaymentReleasePreferences() {
        return listPreferences("""
                SELECT tenant_id, email_enabled, new_sale_enabled, monthly_closing_enabled,
                       ml_payment_release_enabled, weekly_accountant_report_enabled,
                       recipient_email, accountant_email, updated_at
                FROM notification_preferences
                WHERE ml_payment_release_enabled = TRUE
                ORDER BY tenant_id
                """);
    }

    @Override
    public List<NotificationPreference> listWeeklyAccountantReportPreferences() {
        return listPreferences("""
                SELECT tenant_id, email_enabled, new_sale_enabled, monthly_closing_enabled,
                       ml_payment_release_enabled, weekly_accountant_report_enabled,
                       recipient_email, accountant_email, updated_at
                FROM notification_preferences
                WHERE weekly_accountant_report_enabled = TRUE
                  AND email_enabled = TRUE
                  AND accountant_email IS NOT NULL
                ORDER BY tenant_id
                """);
    }

    @Override
    public NotificationMessage save(NotificationMessage notification) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO notifications
                     (id, tenant_id, type, title, message, recipient_email, channel, status, read_at, created_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, notification.id());
            statement.setString(2, notification.tenantId());
            statement.setString(3, notification.type().name());
            statement.setString(4, notification.title());
            statement.setString(5, notification.message());
            statement.setString(6, notification.recipientEmail());
            statement.setString(7, notification.channel().name());
            statement.setString(8, notification.status().name());
            statement.setTimestamp(9, timestamp(notification.readAt()));
            statement.setTimestamp(10, Timestamp.from(notification.createdAt()));
            statement.executeUpdate();
            return notification;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save notification", exception);
        }
    }

    @Override
    public void recordDelivery(String notificationId, NotificationChannel channel, DeliveryStatus status, String errorMessage) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO notification_deliveries
                     (id, notification_id, channel, status, error_message, created_at)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, notificationId);
            statement.setString(3, channel.name());
            statement.setString(4, status.name());
            statement.setString(5, errorMessage);
            statement.setTimestamp(6, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not record notification delivery", exception);
        }
    }

    @Override
    public List<NotificationMessage> list(String tenantId, int limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, tenant_id, type, title, message, recipient_email, channel, status, read_at, created_at
                     FROM notifications
                     WHERE tenant_id = ? AND status <> 'ARCHIVED'
                     ORDER BY created_at DESC
                     LIMIT ?
                     """)) {
            statement.setString(1, tenantId);
            statement.setInt(2, limit);
            List<NotificationMessage> notifications = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(readNotification(resultSet));
                }
            }
            return notifications;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list notifications", exception);
        }
    }

    @Override
    public Optional<NotificationMessage> markAsRead(String tenantId, String notificationId) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE notifications
                    SET status = 'READ', read_at = ?
                    WHERE tenant_id = ? AND id = ?
                    """)) {
                statement.setTimestamp(1, Timestamp.from(Instant.now()));
                statement.setString(2, tenantId);
                statement.setString(3, notificationId);
                statement.executeUpdate();
            }
            return findNotification(connection, tenantId, notificationId);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark notification as read", exception);
        }
    }

    @Override
    public int archiveRead(String tenantId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE notifications
                     SET status = 'ARCHIVED'
                     WHERE tenant_id = ? AND status = 'READ'
                     """)) {
            statement.setString(1, tenantId);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not archive notifications", exception);
        }
    }

    private Optional<NotificationPreference> findPreference(String tenantId) {
        try {
            return getPreferenceRow(tenantId);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find notification preference", exception);
        }
    }

    private int updatePreference(Connection connection, NotificationPreference preference) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE notification_preferences
                SET email_enabled = ?,
                    new_sale_enabled = ?,
                    monthly_closing_enabled = ?,
                    ml_payment_release_enabled = ?,
                    weekly_accountant_report_enabled = ?,
                    recipient_email = ?,
                    accountant_email = ?,
                    updated_at = ?
                WHERE tenant_id = ?
                """)) {
            statement.setBoolean(1, preference.emailEnabled());
            statement.setBoolean(2, preference.newSaleEnabled());
            statement.setBoolean(3, preference.monthlyClosingEnabled());
            statement.setBoolean(4, preference.mlPaymentReleaseEnabled());
            statement.setBoolean(5, preference.weeklyAccountantReportEnabled());
            statement.setString(6, preference.recipientEmail());
            statement.setString(7, preference.accountantEmail());
            statement.setTimestamp(8, Timestamp.from(preference.updatedAt()));
            statement.setString(9, preference.tenantId());
            return statement.executeUpdate();
        }
    }

    private void insertPreference(Connection connection, NotificationPreference preference) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO notification_preferences
                (tenant_id, email_enabled, new_sale_enabled, monthly_closing_enabled,
                 ml_payment_release_enabled, weekly_accountant_report_enabled, recipient_email, accountant_email, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, preference.tenantId());
            statement.setBoolean(2, preference.emailEnabled());
            statement.setBoolean(3, preference.newSaleEnabled());
            statement.setBoolean(4, preference.monthlyClosingEnabled());
            statement.setBoolean(5, preference.mlPaymentReleaseEnabled());
            statement.setBoolean(6, preference.weeklyAccountantReportEnabled());
            statement.setString(7, preference.recipientEmail());
            statement.setString(8, preference.accountantEmail());
            statement.setTimestamp(9, Timestamp.from(preference.updatedAt()));
            statement.executeUpdate();
        }
    }

    private Optional<NotificationPreference> getPreferenceRow(String tenantId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT tenant_id, email_enabled, new_sale_enabled, monthly_closing_enabled,
                            ml_payment_release_enabled, weekly_accountant_report_enabled, recipient_email, accountant_email, updated_at
                     FROM notification_preferences
                     WHERE tenant_id = ?
                     """)) {
            statement.setString(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readPreference(resultSet));
            }
        }
    }

    private List<NotificationPreference> listPreferences(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            List<NotificationPreference> preferences = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    preferences.add(readPreference(resultSet));
                }
            }
            return preferences;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list notification preferences", exception);
        }
    }

    private NotificationPreference readPreference(ResultSet resultSet) throws SQLException {
        return new NotificationPreference(
                resultSet.getString("tenant_id"),
                resultSet.getBoolean("email_enabled"),
                resultSet.getBoolean("new_sale_enabled"),
                resultSet.getBoolean("monthly_closing_enabled"),
                resultSet.getBoolean("ml_payment_release_enabled"),
                resultSet.getBoolean("weekly_accountant_report_enabled"),
                resultSet.getString("recipient_email"),
                resultSet.getString("accountant_email"),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private Optional<NotificationMessage> findNotification(Connection connection, String tenantId, String notificationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, type, title, message, recipient_email, channel, status, read_at, created_at
                FROM notifications
                WHERE tenant_id = ? AND id = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, notificationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readNotification(resultSet));
            }
        }
    }

    private NotificationMessage readNotification(ResultSet resultSet) throws SQLException {
        Timestamp readAt = resultSet.getTimestamp("read_at");
        return new NotificationMessage(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                NotificationType.valueOf(resultSet.getString("type")),
                resultSet.getString("title"),
                resultSet.getString("message"),
                resultSet.getString("recipient_email"),
                NotificationChannel.valueOf(resultSet.getString("channel")),
                NotificationStatus.valueOf(resultSet.getString("status")),
                readAt == null ? null : readAt.toInstant(),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
