package com.example.infrastructure.persistence;

import com.example.application.port.out.BillingRepository;
import com.example.domain.model.BillingPlan;
import com.example.domain.model.BillingPlanCode;
import com.example.domain.model.BillingProvider;
import com.example.domain.model.BillingSubscription;
import com.example.domain.model.BillingWebhookEvent;
import com.example.domain.model.BillingWebhookEventType;
import com.example.domain.model.SubscriptionStatus;
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

@ApplicationScoped
public class JdbcBillingRepository implements BillingRepository {
    @Inject
    DataSource dataSource;

    @Override
    public List<BillingPlan> listPlans() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT code, name, description, monthly_price, currency, trial_days,
                            marketplace_limit, user_limit, active
                     FROM billing_plans
                     ORDER BY sort_order ASC
                     """)) {
            List<BillingPlan> plans = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    plans.add(readPlan(resultSet));
                }
            }
            return plans;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list billing plans", exception);
        }
    }

    @Override
    public Optional<BillingPlan> findPlan(BillingPlanCode code) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT code, name, description, monthly_price, currency, trial_days,
                            marketplace_limit, user_limit, active
                     FROM billing_plans
                     WHERE code = ?
                     """)) {
            statement.setString(1, code.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readPlan(resultSet));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find billing plan", exception);
        }
    }

    @Override
    public Optional<BillingSubscription> findSubscriptionByTenantId(String tenantId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, tenant_id, plan_code, status, provider, provider_customer_id,
                            provider_subscription_id, trial_started_at, trial_ends_at,
                            current_period_started_at, current_period_ends_at, suspended_at,
                            cancellation_reason, latest_event_id, created_at, updated_at
                     FROM billing_subscriptions
                     WHERE tenant_id = ?
                     """)) {
            statement.setString(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readSubscription(resultSet));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find billing subscription", exception);
        }
    }

    @Override
    public BillingSubscription insertSubscription(BillingSubscription subscription) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO billing_subscriptions
                     (id, tenant_id, plan_code, status, provider, provider_customer_id,
                      provider_subscription_id, trial_started_at, trial_ends_at,
                      current_period_started_at, current_period_ends_at, suspended_at,
                      cancellation_reason, latest_event_id, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            bindSubscription(statement, subscription);
            statement.executeUpdate();
            return findSubscriptionByTenantId(subscription.tenantId())
                    .orElseThrow(() -> new RepositoryException("Could not find inserted subscription", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not insert billing subscription", exception);
        }
    }

    @Override
    public BillingSubscription updateSubscription(BillingSubscription subscription) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE billing_subscriptions
                     SET plan_code = ?,
                         status = ?,
                         provider = ?,
                         provider_customer_id = ?,
                         provider_subscription_id = ?,
                         trial_started_at = ?,
                         trial_ends_at = ?,
                         current_period_started_at = ?,
                         current_period_ends_at = ?,
                         suspended_at = ?,
                         cancellation_reason = ?,
                         latest_event_id = ?,
                         updated_at = ?
                     WHERE tenant_id = ?
                     """)) {
            statement.setString(1, subscription.planCode().name());
            statement.setString(2, subscription.status().name());
            statement.setString(3, subscription.provider().name());
            statement.setString(4, subscription.providerCustomerId());
            statement.setString(5, subscription.providerSubscriptionId());
            statement.setTimestamp(6, timestamp(subscription.trialStartedAt()));
            statement.setTimestamp(7, timestamp(subscription.trialEndsAt()));
            statement.setTimestamp(8, timestamp(subscription.currentPeriodStartedAt()));
            statement.setTimestamp(9, timestamp(subscription.currentPeriodEndsAt()));
            statement.setTimestamp(10, timestamp(subscription.suspendedAt()));
            statement.setString(11, subscription.cancellationReason());
            statement.setString(12, subscription.latestEventId());
            statement.setTimestamp(13, timestamp(subscription.updatedAt()));
            statement.setString(14, subscription.tenantId());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("Could not update missing billing subscription", null);
            }
            return findSubscriptionByTenantId(subscription.tenantId())
                    .orElseThrow(() -> new RepositoryException("Could not find updated subscription", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not update billing subscription", exception);
        }
    }

    @Override
    public boolean webhookEventExists(String provider, String providerEventId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM billing_webhook_events
                     WHERE provider = ? AND provider_event_id = ?
                     """)) {
            statement.setString(1, provider);
            statement.setString(2, providerEventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not check billing webhook event", exception);
        }
    }

    @Override
    public void insertWebhookEvent(BillingWebhookEvent event) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO billing_webhook_events
                     (id, provider, provider_event_id, tenant_id, event_type, status, received_at, payload)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, event.id());
            statement.setString(2, event.provider().name());
            statement.setString(3, event.providerEventId());
            statement.setString(4, event.tenantId());
            statement.setString(5, event.eventType().name());
            statement.setString(6, event.status());
            statement.setTimestamp(7, timestamp(event.receivedAt()));
            statement.setString(8, event.payload());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not insert billing webhook event", exception);
        }
    }

    private void bindSubscription(PreparedStatement statement, BillingSubscription subscription) throws SQLException {
        statement.setString(1, subscription.id());
        statement.setString(2, subscription.tenantId());
        statement.setString(3, subscription.planCode().name());
        statement.setString(4, subscription.status().name());
        statement.setString(5, subscription.provider().name());
        statement.setString(6, subscription.providerCustomerId());
        statement.setString(7, subscription.providerSubscriptionId());
        statement.setTimestamp(8, timestamp(subscription.trialStartedAt()));
        statement.setTimestamp(9, timestamp(subscription.trialEndsAt()));
        statement.setTimestamp(10, timestamp(subscription.currentPeriodStartedAt()));
        statement.setTimestamp(11, timestamp(subscription.currentPeriodEndsAt()));
        statement.setTimestamp(12, timestamp(subscription.suspendedAt()));
        statement.setString(13, subscription.cancellationReason());
        statement.setString(14, subscription.latestEventId());
        statement.setTimestamp(15, timestamp(subscription.createdAt()));
        statement.setTimestamp(16, timestamp(subscription.updatedAt()));
    }

    private BillingPlan readPlan(ResultSet resultSet) throws SQLException {
        return new BillingPlan(
                BillingPlanCode.valueOf(resultSet.getString("code")),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("monthly_price"),
                resultSet.getString("currency"),
                resultSet.getInt("trial_days"),
                resultSet.getInt("marketplace_limit"),
                resultSet.getInt("user_limit"),
                resultSet.getBoolean("active")
        );
    }

    private BillingSubscription readSubscription(ResultSet resultSet) throws SQLException {
        return new BillingSubscription(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                BillingPlanCode.valueOf(resultSet.getString("plan_code")),
                SubscriptionStatus.valueOf(resultSet.getString("status")),
                BillingProvider.valueOf(resultSet.getString("provider")),
                resultSet.getString("provider_customer_id"),
                resultSet.getString("provider_subscription_id"),
                instant(resultSet, "trial_started_at"),
                instant(resultSet, "trial_ends_at"),
                instant(resultSet, "current_period_started_at"),
                instant(resultSet, "current_period_ends_at"),
                instant(resultSet, "suspended_at"),
                resultSet.getString("cancellation_reason"),
                resultSet.getString("latest_event_id"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
