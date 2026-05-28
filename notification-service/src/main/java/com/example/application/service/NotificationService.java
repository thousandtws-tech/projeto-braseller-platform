package com.example.application.service;

import com.example.application.command.MonthlyClosingNotificationCommand;
import com.example.application.command.MlPaymentReleaseNotificationCommand;
import com.example.application.command.NewSaleNotificationCommand;
import com.example.application.command.UpdateNotificationPreferenceCommand;
import com.example.application.command.WeeklyAccountantReportCommand;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.NotificationEmailSender;
import com.example.application.port.out.NotificationRepository;
import com.example.domain.model.DeliveryStatus;
import com.example.domain.model.NotificationChannel;
import com.example.domain.model.NotificationMessage;
import com.example.domain.model.NotificationPreference;
import com.example.domain.model.NotificationStatus;
import com.example.domain.model.NotificationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NotificationService {
    private static final int DEFAULT_LIMIT = 50;

    private final NotificationRepository repository;
    private final NotificationEmailSender emailSender;

    @Inject
    public NotificationService(NotificationRepository repository, NotificationEmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    public NotificationPreference getPreference(String tenantId) {
        return repository.getPreference(requireText(tenantId, "tenantId"));
    }

    public NotificationPreference updatePreference(UpdateNotificationPreferenceCommand command) {
        String tenantId = requireText(command.tenantId(), "tenantId");
        NotificationPreference current = repository.getPreference(tenantId);
        NotificationPreference updated = new NotificationPreference(
                tenantId,
                valueOrDefault(command.emailEnabled(), current.emailEnabled()),
                valueOrDefault(command.newSaleEnabled(), current.newSaleEnabled()),
                valueOrDefault(command.monthlyClosingEnabled(), current.monthlyClosingEnabled()),
                valueOrDefault(command.mlPaymentReleaseEnabled(), current.mlPaymentReleaseEnabled()),
                valueOrDefault(command.weeklyAccountantReportEnabled(), current.weeklyAccountantReportEnabled()),
                firstNonBlank(command.recipientEmail(), current.recipientEmail()),
                firstNonBlank(command.accountantEmail(), current.accountantEmail()),
                Instant.now()
        );
        return repository.savePreference(updated);
    }

    public List<NotificationMessage> list(String tenantId, Integer limit) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, 100));
        return repository.list(requireText(tenantId, "tenantId"), safeLimit);
    }

    public Optional<NotificationMessage> markAsRead(String tenantId, String notificationId) {
        return repository.markAsRead(requireText(tenantId, "tenantId"), requireText(notificationId, "notificationId"));
    }

    public int archiveRead(String tenantId) {
        return repository.archiveRead(requireText(tenantId, "tenantId"));
    }

    public Optional<NotificationMessage> notifyNewSale(NewSaleNotificationCommand command) {
        String tenantId = requireText(command.tenantId(), "tenantId");
        NotificationPreference preference = repository.getPreference(tenantId);
        if (!preference.newSaleEnabled()) {
            return Optional.empty();
        }

        String marketplace = defaultText(command.marketplace(), "Marketplace");
        String orderId = requireText(command.orderId(), "orderId");
        String title = "Nova venda recebida";
        String message = "Venda " + orderId + " em " + marketplace + " no valor de " + money(command.amount()) + ".";
        return Optional.of(createAndDeliver(tenantId, command.recipientEmail(), NotificationType.NEW_SALE, title, message, preference));
    }

    public Optional<NotificationMessage> notifyMlPaymentRelease(MlPaymentReleaseNotificationCommand command) {
        String tenantId = requireText(command.tenantId(), "tenantId");
        NotificationPreference preference = repository.getPreference(tenantId);
        if (!preference.mlPaymentReleaseEnabled()) {
            return Optional.empty();
        }

        LocalDate releaseDate = command.releaseDate() == null ? LocalDate.now() : command.releaseDate();
        String title = "Pagamento do Mercado Livre proximo de liberar";
        String message = "Pagamento " + requireText(command.paymentId(), "paymentId")
                + " de " + money(command.amount())
                + " previsto para liberar em " + releaseDate + ".";
        return Optional.of(createAndDeliver(tenantId, command.recipientEmail(), NotificationType.ML_PAYMENT_RELEASE_SOON, title, message, preference));
    }

    public Optional<NotificationMessage> sendMonthlyClosing(MonthlyClosingNotificationCommand command) {
        String tenantId = requireText(command.tenantId(), "tenantId");
        NotificationPreference preference = repository.getPreference(tenantId);
        if (!preference.monthlyClosingEnabled()) {
            return Optional.empty();
        }

        YearMonth period = command.period() == null ? YearMonth.now().minusMonths(1) : command.period();
        String title = "Fechamento mensal disponivel";
        String message = "Resumo de " + period + ": " + command.totalSales()
                + " vendas e faturamento bruto de " + money(command.grossRevenue()) + ".";
        return Optional.of(createAndDeliver(tenantId, command.recipientEmail(), NotificationType.MONTHLY_CLOSING_SUMMARY, title, message, preference));
    }

    public Optional<NotificationMessage> sendWeeklyAccountantReport(WeeklyAccountantReportCommand command) {
        String tenantId = requireText(command.tenantId(), "tenantId");
        NotificationPreference preference = repository.getPreference(tenantId);
        if (!preference.weeklyAccountantReportEnabled()) {
            return Optional.empty();
        }

        String recipient = firstNonBlank(command.accountantEmail(), preference.accountantEmail());
        if (recipient == null) {
            throw new ValidationException("accountantEmail is required");
        }

        LocalDate weekStart = command.weekStart() == null ? LocalDate.now().minusDays(7) : command.weekStart();
        LocalDate weekEnd = command.weekEnd() == null ? LocalDate.now() : command.weekEnd();
        String title = "Relatorio semanal para contador";
        String message = "Periodo " + weekStart + " a " + weekEnd + ": " + command.totalSales()
                + " vendas e faturamento bruto de " + money(command.grossRevenue()) + ".";
        return Optional.of(createAndDeliver(tenantId, recipient, NotificationType.WEEKLY_ACCOUNTANT_REPORT, title, message, preference));
    }

    private NotificationMessage createAndDeliver(
            String tenantId,
            String recipientEmail,
            NotificationType type,
            String title,
            String message,
            NotificationPreference preference) {
        NotificationMessage notification = repository.save(new NotificationMessage(
                UUID.randomUUID().toString(),
                tenantId,
                type,
                title,
                message,
                blankToNull(recipientEmail),
                NotificationChannel.IN_APP,
                NotificationStatus.UNREAD,
                null,
                Instant.now()
        ));

        deliverByEmail(notification, preference);
        return notification;
    }

    private void deliverByEmail(NotificationMessage notification, NotificationPreference preference) {
        if (!preference.emailEnabled() || notification.recipientEmail() == null) {
            repository.recordDelivery(notification.id(), NotificationChannel.EMAIL, DeliveryStatus.SKIPPED, null);
            return;
        }

        try {
            emailSender.send(notification);
            repository.recordDelivery(notification.id(), NotificationChannel.EMAIL, DeliveryStatus.SENT, null);
        } catch (RuntimeException exception) {
            repository.recordDelivery(notification.id(), NotificationChannel.EMAIL, DeliveryStatus.FAILED, exception.getMessage());
        }
    }

    private boolean valueOrDefault(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = blankToNull(first);
        return normalizedFirst == null ? blankToNull(second) : normalizedFirst;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        DecimalFormat formatter = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        return "BRL " + formatter.format(amount);
    }
}
