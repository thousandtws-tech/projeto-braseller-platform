package com.example.application.port.out;

import com.example.application.event.DreCalculationRequestedEvent;

public interface DreCalculationEventPublisher {
    void publish(DreCalculationRequestedEvent event);
}
