package com.example.application.port.out;

import com.example.domain.model.DeliveryStatus;
import com.example.domain.model.NotificationChannel;
import com.example.domain.model.NotificationMessage;
import com.example.domain.model.NotificationPreference;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    NotificationPreference getPreference(String tenantId);

    NotificationPreference savePreference(NotificationPreference preference);

    NotificationMessage save(NotificationMessage notification);

    void recordDelivery(String notificationId, NotificationChannel channel, DeliveryStatus status, String errorMessage);

    List<NotificationMessage> list(String tenantId, int limit);

    Optional<NotificationMessage> markAsRead(String tenantId, String notificationId);

    int archiveRead(String tenantId);
}
