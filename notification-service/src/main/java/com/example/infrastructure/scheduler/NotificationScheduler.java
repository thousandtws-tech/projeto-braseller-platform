package com.example.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationScheduler {
    private static final Logger LOG = Logger.getLogger(NotificationScheduler.class);

    @Scheduled(cron = "{notification.jobs.monthly-closing.cron}")
    void monthlyClosingJob() {
        LOG.info("monthly_closing_notification_job_tick");
    }

    @Scheduled(cron = "{notification.jobs.ml-payment-release.cron}")
    void mlPaymentReleaseJob() {
        LOG.info("ml_payment_release_notification_job_tick");
    }

    @Scheduled(cron = "{notification.jobs.weekly-accountant-report.cron}")
    void weeklyAccountantReportJob() {
        LOG.info("weekly_accountant_report_notification_job_tick");
    }
}
