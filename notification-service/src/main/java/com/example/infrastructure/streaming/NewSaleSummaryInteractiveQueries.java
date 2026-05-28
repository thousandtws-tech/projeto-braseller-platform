package com.example.infrastructure.streaming;

import com.example.application.port.out.NewSaleSummaryQuery;
import com.example.domain.model.TenantNewSaleSummary;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
@UnlessBuildProfile("test")
public class NewSaleSummaryInteractiveQueries implements NewSaleSummaryQuery {
    private static final Duration STORE_WAIT_TIMEOUT = Duration.ofSeconds(2);

    @Inject
    KafkaStreams streams;

    @Override
    public Optional<TenantNewSaleSummary> getTenantSummary(String tenantId) {
        return Optional.ofNullable(getSummaryStore().get(tenantId));
    }

    private ReadOnlyKeyValueStore<String, TenantNewSaleSummary> getSummaryStore() {
        long deadline = System.nanoTime() + STORE_WAIT_TIMEOUT.toNanos();
        InvalidStateStoreException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(
                        NewSaleStreamsTopology.TENANT_NEW_SALE_SUMMARY_STORE,
                        QueryableStoreTypes.keyValueStore()
                ));
            } catch (InvalidStateStoreException exception) {
                lastFailure = exception;
                waitBeforeRetry();
            }
        }
        throw new IllegalStateException("kafka_stream_store_not_ready", lastFailure);
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("kafka_stream_store_not_ready", exception);
        }
    }
}
