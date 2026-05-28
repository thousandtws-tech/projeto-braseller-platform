package com.example;

import com.example.application.port.out.NotificationEmailSender;
import com.example.application.port.out.NotificationRepository;
import com.example.application.port.out.ReportingDataProvider;
import com.example.application.service.NotificationService;
import com.example.application.service.ScheduledNotificationService;
import com.example.domain.model.DeliveryStatus;
import com.example.domain.model.NotificationChannel;
import com.example.domain.model.NotificationMessage;
import com.example.domain.model.NotificationPreference;
import com.example.domain.model.NotificationStatus;
import com.example.domain.model.NotificationType;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.ReportingFinancialSummary;
import com.example.domain.model.ScheduledNotificationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class ScheduledNotificationServiceTest {
    @Test
    void scheduledJobsCreateMonthlyMlAndWeeklyNotificationsFromReportingData() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.preferences.add(new NotificationPreference(
                "tenant-auto",
                true,
                true,
                true,
                true,
                true,
                "seller@example.com",
                "contador@example.com",
                Instant.now()
        ));
        FakeReportingDataProvider reporting = new FakeReportingDataProvider();
        NotificationService notificationService = new NotificationService(repository, new FakeEmailSender());
        ScheduledNotificationService scheduledService = new ScheduledNotificationService(repository, notificationService, reporting);

        ScheduledNotificationResult monthly = scheduledService.sendMonthlyClosingNotifications();
        ScheduledNotificationResult ml = scheduledService.sendMlPaymentReleaseAlerts(2);
        ScheduledNotificationResult weekly = scheduledService.sendWeeklyAccountantReports();

        assertThat(monthly.sent(), is(1));
        assertThat(ml.sent(), is(1));
        assertThat(weekly.sent(), is(1));
        assertThat(repository.notifications, hasSize(3));
        assertThat(repository.notifications.stream().map(NotificationMessage::type).toList(), contains(
                NotificationType.MONTHLY_CLOSING_SUMMARY,
                NotificationType.ML_PAYMENT_RELEASE_SOON,
                NotificationType.WEEKLY_ACCOUNTANT_REPORT
        ));
        assertThat(repository.deliveries, hasSize(3));
    }

    private static class FakeNotificationRepository implements NotificationRepository {
        private final List<NotificationPreference> preferences = new ArrayList<>();
        private final List<NotificationMessage> notifications = new ArrayList<>();
        private final List<DeliveryStatus> deliveries = new ArrayList<>();

        @Override
        public NotificationPreference getPreference(String tenantId) {
            return preferences.stream()
                    .filter(preference -> preference.tenantId().equals(tenantId))
                    .findFirst()
                    .orElseGet(() -> NotificationPreference.defaults(tenantId));
        }

        @Override
        public NotificationPreference savePreference(NotificationPreference preference) {
            preferences.removeIf(existing -> existing.tenantId().equals(preference.tenantId()));
            preferences.add(preference);
            return preference;
        }

        @Override
        public List<NotificationPreference> listMonthlyClosingPreferences() {
            return preferences.stream()
                    .filter(NotificationPreference::monthlyClosingEnabled)
                    .toList();
        }

        @Override
        public List<NotificationPreference> listMlPaymentReleasePreferences() {
            return preferences.stream()
                    .filter(NotificationPreference::mlPaymentReleaseEnabled)
                    .toList();
        }

        @Override
        public List<NotificationPreference> listWeeklyAccountantReportPreferences() {
            return preferences.stream()
                    .filter(NotificationPreference::weeklyAccountantReportEnabled)
                    .toList();
        }

        @Override
        public NotificationMessage save(NotificationMessage notification) {
            notifications.add(notification);
            return notification;
        }

        @Override
        public void recordDelivery(String notificationId, NotificationChannel channel, DeliveryStatus status, String errorMessage) {
            deliveries.add(status);
        }

        @Override
        public List<NotificationMessage> list(String tenantId, int limit) {
            return notifications.stream()
                    .filter(notification -> notification.tenantId().equals(tenantId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<NotificationMessage> markAsRead(String tenantId, String notificationId) {
            return Optional.empty();
        }

        @Override
        public int archiveRead(String tenantId) {
            return 0;
        }
    }

    private static class FakeReportingDataProvider implements ReportingDataProvider {
        @Override
        public ReportingFinancialSummary summary(String tenantId, LocalDate from, LocalDate to) {
            return new ReportingFinancialSummary(new BigDecimal("1234.56"), 12);
        }

        @Override
        public List<PaymentReleaseAlert> paymentReleases(String tenantId, String platform, LocalDate from, LocalDate to) {
            return List.of(new PaymentReleaseAlert(
                    tenantId,
                    platform,
                    "ML-123",
                    new BigDecimal("199.90"),
                    from.plusDays(1)
            ));
        }
    }

    private static class FakeEmailSender implements NotificationEmailSender {
        @Override
        public void send(NotificationMessage notification) {
        }
    }
}
