package com.example.infrastructure.mail;

import com.example.application.port.out.NotificationEmailSender;
import com.example.domain.model.NotificationMessage;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MailerNotificationEmailSender implements NotificationEmailSender {
    @Inject
    Mailer mailer;

    @Inject
    Template notificationEmail;

    @ConfigProperty(name = "notification.mail.from")
    String from;

    @Override
    public void send(NotificationMessage notification) {
        String html = notificationEmail
                .data("title", notification.title())
                .data("message", notification.message())
                .data("type", notification.type().name())
                .data("tenantId", notification.tenantId())
                .render();

        mailer.send(Mail.withHtml(notification.recipientEmail(), notification.title(), html)
                .setFrom(from)
                .setText(notification.message()));
    }
}
