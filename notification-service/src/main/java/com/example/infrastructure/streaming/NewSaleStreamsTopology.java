package com.example.infrastructure.streaming;

import com.example.domain.model.TenantNewSaleSummary;
import com.example.infrastructure.messaging.NewSaleEvent;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@UnlessBuildProfile("test")
public class NewSaleStreamsTopology {
    public static final String TENANT_NEW_SALE_SUMMARY_STORE = "tenant-new-sale-summary-store";

    @ConfigProperty(name = "mp.messaging.incoming.new-sale-events-in.topic")
    String newSaleEventsTopic;

    @ConfigProperty(name = "notification.kafka.topic.new-sale-summary")
    String newSaleSummaryTopic;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        ObjectMapperSerde<NewSaleEvent> newSaleEventSerde = new ObjectMapperSerde<>(NewSaleEvent.class);
        ObjectMapperSerde<TenantNewSaleSummary> summarySerde = new ObjectMapperSerde<>(TenantNewSaleSummary.class);
        KeyValueBytesStoreSupplier store = Stores.persistentKeyValueStore(TENANT_NEW_SALE_SUMMARY_STORE);

        KStream<String, NewSaleEvent> newSaleEvents = builder.stream(
                newSaleEventsTopic,
                Consumed.with(Serdes.String(), newSaleEventSerde)
        );

        KTable<String, TenantNewSaleSummary> tenantSummaries = newSaleEvents
                .filter((key, event) -> event != null && hasText(event.tenantId()))
                .selectKey((key, event) -> event.tenantId())
                .groupByKey(Grouped.with(Serdes.String(), newSaleEventSerde))
                .aggregate(
                        TenantNewSaleSummary::empty,
                        (tenantId, event, summary) -> (summary == null ? TenantNewSaleSummary.empty(tenantId) : summary).add(
                                tenantId,
                                event.marketplace(),
                                event.orderId(),
                                event.eventId(),
                                event.amount(),
                                event.occurredAt()
                        ),
                        Materialized.<String, TenantNewSaleSummary>as(store)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(summarySerde)
                );

        tenantSummaries.toStream().to(newSaleSummaryTopic, Produced.with(Serdes.String(), summarySerde));
        return builder.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
