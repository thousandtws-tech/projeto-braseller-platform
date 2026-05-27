package com.example;

import com.example.domain.model.TenantNewSaleSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;

class TenantNewSaleSummaryTest {
    @Test
    void accumulatesNewSaleEventsByTenant() {
        Instant firstEventAt = Instant.parse("2026-05-27T09:00:00Z");
        Instant secondEventAt = Instant.parse("2026-05-27T09:10:00Z");

        TenantNewSaleSummary summary = TenantNewSaleSummary.empty("tenant-a")
                .add("tenant-a", "sandbox", "ORDER-1", "event-1", new BigDecimal("100.25"), firstEventAt)
                .add("tenant-a", "sandbox", "ORDER-2", "event-2", new BigDecimal("99.65"), secondEventAt);

        assertThat(summary.tenantId(), is("tenant-a"));
        assertThat(summary.saleCount(), is(2L));
        assertThat(summary.grossRevenue(), comparesEqualTo(new BigDecimal("199.90")));
        assertThat(summary.lastOrderId(), is("ORDER-2"));
        assertThat(summary.lastEventId(), is("event-2"));
        assertThat(summary.lastEventAt(), is(secondEventAt));
    }
}
