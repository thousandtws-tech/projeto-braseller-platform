package com.example.application.port.out;

import com.example.domain.model.DeliveryStatus;
import com.example.domain.model.NotificationChannel;
import com.example.domain.model.NotificationMessage;
import com.example.domain.model.NotificationPreference;
import com.example.domain.model.TenantNewSaleSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    NotificationPreference getPreference(String tenantId);

    NotificationPreference savePreference(NotificationPreference preference);

    List<NotificationPreference> listMonthlyClosingPreferences();

    List<NotificationPreference> listMlPaymentReleasePreferences();

    List<NotificationPreference> listWeeklyAccountantReportPreferences();

    NotificationMessage save(NotificationMessage notification);

    boolean recordNewSaleEvent(String eventId, String tenantId, String marketplace, String orderId, BigDecimal amount, Instant occurredAt);

    Optional<TenantNewSaleSummary> findNewSaleSummary(String tenantId);

    void recordDelivery(String notificationId, NotificationChannel channel, DeliveryStatus status, String errorMessage);

    List<NotificationMessage> list(String tenantId, int limit);

    Optional<NotificationMessage> markAsRead(String tenantId, String notificationId);

    int archiveRead(String tenantId);
}
