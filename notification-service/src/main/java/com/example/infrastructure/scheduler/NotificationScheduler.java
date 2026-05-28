package com.example.infrastructure.scheduler;

import com.example.application.service.ScheduledNotificationService;
import com.example.domain.model.ScheduledNotificationResult;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationScheduler {
    private static final Logger LOG = Logger.getLogger(NotificationScheduler.class);

    @Inject
    ScheduledNotificationService scheduledNotificationService;

    @ConfigProperty(name = "notification.jobs.ml-payment-release-lookahead-days")
    int mlPaymentReleaseLookaheadDays;

    @Scheduled(cron = "{notification.jobs.monthly-closing.cron}")
    void monthlyClosingJob() {
        ScheduledNotificationResult result = scheduledNotificationService.sendMonthlyClosingNotifications();
        LOG.infof("monthly_closing_notification_job_processed candidates=%d sent=%d skipped=%d failed=%d",
                result.candidates(), result.sent(), result.skipped(), result.failed());
    }

    @Scheduled(cron = "{notification.jobs.ml-payment-release.cron}")
    void mlPaymentReleaseJob() {
        ScheduledNotificationResult result = scheduledNotificationService.sendMlPaymentReleaseAlerts(mlPaymentReleaseLookaheadDays);
        LOG.infof("ml_payment_release_notification_job_processed candidates=%d sent=%d skipped=%d failed=%d",
                result.candidates(), result.sent(), result.skipped(), result.failed());
    }

    @Scheduled(cron = "{notification.jobs.weekly-accountant-report.cron}")
    void weeklyAccountantReportJob() {
        ScheduledNotificationResult result = scheduledNotificationService.sendWeeklyAccountantReports();
        LOG.infof("weekly_accountant_report_notification_job_processed candidates=%d sent=%d skipped=%d failed=%d",
                result.candidates(), result.sent(), result.skipped(), result.failed());
    }
}
