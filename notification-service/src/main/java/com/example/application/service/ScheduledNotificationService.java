package com.example.application.service;

import com.example.application.command.MlPaymentReleaseNotificationCommand;
import com.example.application.command.MonthlyClosingNotificationCommand;
import com.example.application.command.WeeklyAccountantReportCommand;
import com.example.application.port.out.NotificationRepository;
import com.example.application.port.out.ReportingDataProvider;
import com.example.domain.model.NotificationPreference;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.ReportingFinancialSummary;
import com.example.domain.model.ScheduledNotificationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@ApplicationScoped
public class ScheduledNotificationService {
    private static final Logger LOG = Logger.getLogger(ScheduledNotificationService.class);

    private final NotificationRepository repository;
    private final NotificationService notificationService;
    private final ReportingDataProvider reportingDataProvider;

    @Inject
    public ScheduledNotificationService(
            NotificationRepository repository,
            NotificationService notificationService,
            ReportingDataProvider reportingDataProvider) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.reportingDataProvider = reportingDataProvider;
    }

    public ScheduledNotificationResult sendMonthlyClosingNotifications() {
        YearMonth period = YearMonth.now().minusMonths(1);
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        List<NotificationPreference> preferences = repository.listMonthlyClosingPreferences();
        for (NotificationPreference preference : preferences) {
            try {
                ReportingFinancialSummary summary = reportingDataProvider.summary(preference.tenantId(), from, to);
                if (notificationService.sendMonthlyClosing(new MonthlyClosingNotificationCommand(
                        preference.tenantId(),
                        preference.recipientEmail(),
                        period,
                        cappedEntryCount(summary.entryCount()),
                        summary.grossValue()
                )).isPresent()) {
                    sent++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException exception) {
                failed++;
                LOG.warnf(exception, "Failed to send monthly closing notification for tenant %s", preference.tenantId());
            }
        }
        return new ScheduledNotificationResult(preferences.size(), sent, skipped, failed);
    }

    public ScheduledNotificationResult sendMlPaymentReleaseAlerts(int lookaheadDays) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(Math.max(0, lookaheadDays));
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        int candidates = 0;
        for (NotificationPreference preference : repository.listMlPaymentReleasePreferences()) {
            try {
                List<PaymentReleaseAlert> releases = reportingDataProvider.paymentReleases(
                        preference.tenantId(),
                        "mercado-livre",
                        from,
                        to
                );
                candidates += releases.size();
                for (PaymentReleaseAlert release : releases) {
                    if (notificationService.notifyMlPaymentRelease(new MlPaymentReleaseNotificationCommand(
                            preference.tenantId(),
                            preference.recipientEmail(),
                            release.paymentId(),
                            release.amount(),
                            release.releaseDate()
                    )).isPresent()) {
                        sent++;
                    } else {
                        skipped++;
                    }
                }
            } catch (RuntimeException exception) {
                failed++;
                LOG.warnf(exception, "Failed to send ML payment release alerts for tenant %s", preference.tenantId());
            }
        }
        return new ScheduledNotificationResult(candidates, sent, skipped, failed);
    }

    public ScheduledNotificationResult sendWeeklyAccountantReports() {
        LocalDate weekEnd = LocalDate.now();
        LocalDate weekStart = weekEnd.minusDays(7);
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        List<NotificationPreference> preferences = repository.listWeeklyAccountantReportPreferences();
        for (NotificationPreference preference : preferences) {
            try {
                ReportingFinancialSummary summary = reportingDataProvider.summary(preference.tenantId(), weekStart, weekEnd);
                if (notificationService.sendWeeklyAccountantReport(new WeeklyAccountantReportCommand(
                        preference.tenantId(),
                        preference.accountantEmail(),
                        weekStart,
                        weekEnd,
                        cappedEntryCount(summary.entryCount()),
                        summary.grossValue()
                )).isPresent()) {
                    sent++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException exception) {
                failed++;
                LOG.warnf(exception, "Failed to send weekly accountant report for tenant %s", preference.tenantId());
            }
        }
        return new ScheduledNotificationResult(preferences.size(), sent, skipped, failed);
    }

    private int cappedEntryCount(long entryCount) {
        if (entryCount > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (entryCount < 0) {
            return 0;
        }
        return (int) entryCount;
    }
}
