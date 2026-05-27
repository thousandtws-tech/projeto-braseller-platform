package com.example.application.port.out;

import com.example.application.event.NewSaleEvent;

public interface DomainEventPublisher {
    void publishNewSale(NewSaleEvent event);
}
