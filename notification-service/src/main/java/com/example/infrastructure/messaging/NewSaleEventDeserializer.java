package com.example.infrastructure.messaging;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class NewSaleEventDeserializer extends ObjectMapperDeserializer<NewSaleEvent> {
    public NewSaleEventDeserializer() {
        super(NewSaleEvent.class);
    }
}
