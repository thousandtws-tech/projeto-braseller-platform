package com.example.application.port.out;

import com.example.domain.model.NotificationMessage;

public interface NotificationEmailSender {
    void send(NotificationMessage notification);
}
