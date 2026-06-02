package com.example.application.port.out;

import com.example.domain.model.ClicksignWebhookEvent;

public interface ClicksignWebhookEventRepository {
    ClicksignWebhookEvent save(ClicksignWebhookEvent event, String payloadJson, String contentHmac);
}
