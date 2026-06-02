package com.example.application.event;

import com.example.domain.model.connector.OrderStatus;
import com.example.domain.model.connector.PaymentMethod;
import com.example.domain.model.connector.StandardOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReportEntryUpsertRequestedEvent(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("platform") String platform,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("sale_date") LocalDate saleDate,
        @JsonProperty("gross_value") BigDecimal grossValue,
        @JsonProperty("received_value") BigDecimal receivedValue,
        @JsonProperty("fee_value") BigDecimal feeValue,
        @JsonProperty("receivable_value") BigDecimal receivableValue,
        @JsonProperty("payment_method") String paymentMethod,
        @JsonProperty("status") String status,
        @JsonProperty("release_date") LocalDate releaseDate,
        @JsonProperty("buyer_name") String buyerName,
        @JsonProperty("invoice_number") String invoiceNumber) {

    public static ReportEntryUpsertRequestedEvent fromOrder(String tenantId, StandardOrder order) {
        BigDecimal netValue = money(order.netValue());
        String status = reportStatus(order);
        return new ReportEntryUpsertRequestedEvent(
                UUID.randomUUID().toString(),
                "reporting.report-entry-upsert-requested.v1",
                Instant.now(),
                tenantId,
                order.platform(),
                order.orderId(),
                order.date(),
                money(order.grossValue()),
                receivedValue(netValue, status),
                money(order.platformFee()),
                receivableValue(netValue, status),
                paymentMethod(order.paymentMethod()),
                status,
                order.releaseDate(),
                order.buyerName(),
                order.invoiceNumber()
        );
    }

    private static String reportStatus(StandardOrder order) {
        if (order.status() == OrderStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (order.status() == OrderStatus.PENDING) {
            return "PENDING_RELEASE";
        }
        LocalDate releaseDate = order.releaseDate();
        if (releaseDate != null && !releaseDate.isAfter(LocalDate.now())) {
            return "RECEIVED";
        }
        return "PAID";
    }

    private static BigDecimal receivedValue(BigDecimal netValue, String status) {
        return "RECEIVED".equals(status) ? netValue : BigDecimal.ZERO;
    }

    private static BigDecimal receivableValue(BigDecimal netValue, String status) {
        return "PAID".equals(status) || "PENDING_RELEASE".equals(status) ? netValue : BigDecimal.ZERO;
    }

    private static String paymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "OTHER";
        }
        return switch (paymentMethod) {
            case PIX -> "PIX";
            case CARD -> "CREDIT_CARD";
            case BOLETO -> "BOLETO";
            case MARKETPLACE_BALANCE -> "MARKETPLACE_BALANCE";
            case UNKNOWN -> "OTHER";
        };
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
