package com.example.application.event;

import com.example.domain.enums.PaymentMethod;
import com.example.domain.model.connector.FeeInfo;
import com.example.domain.model.connector.OrderStatus;
import com.example.domain.model.connector.StandardOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
        @JsonProperty("items") List<ReportEntryItem> items,
        @JsonProperty("invoice_number") String invoiceNumber) {

    public ReportEntryUpsertRequestedEvent {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static ReportEntryUpsertRequestedEvent fromOrder(String tenantId, StandardOrder order) {
        return fromOrder(tenantId, order, null);
    }

    public static ReportEntryUpsertRequestedEvent fromOrder(String tenantId, StandardOrder order, List<FeeInfo> fees) {
        BigDecimal grossValue = money(order.grossValue());
        BigDecimal feeValue = effectiveFeeValue(order, fees);
        BigDecimal netValue = grossValue.subtract(feeValue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String status = reportStatus(order);
        return new ReportEntryUpsertRequestedEvent(
                UUID.randomUUID().toString(),
                "reporting.report-entry-upsert-requested.v1",
                Instant.now(),
                tenantId,
                order.platform(),
                order.orderId(),
                order.date(),
                grossValue,
                receivedValue(netValue, status),
                feeValue,
                receivableValue(netValue, status),
                paymentMethod(order.paymentMethod()),
                status,
                order.releaseDate(),
                order.buyerName(),
                items(order),
                order.invoiceNumber()
        );
    }

    private static BigDecimal effectiveFeeValue(StandardOrder order, List<FeeInfo> fees) {
        BigDecimal feeTotal = BigDecimal.ZERO;
        if (fees != null) {
            for (FeeInfo fee : fees) {
                if (fee != null && fee.hasAmount()) {
                    feeTotal = feeTotal.add(fee.amount());
                }
            }
        }
        return money(feeTotal.compareTo(BigDecimal.ZERO) > 0 ? feeTotal : order.platformFee());
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
        return "RECEIVED".equals(status) ? money(netValue) : money(BigDecimal.ZERO);
    }

    private static BigDecimal receivableValue(BigDecimal netValue, String status) {
        return "PAID".equals(status) || "PENDING_RELEASE".equals(status) ? money(netValue) : money(BigDecimal.ZERO);
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

    private static List<ReportEntryItem> items(StandardOrder order) {
        if (order.items() == null) {
            return List.of();
        }
        return order.items().stream()
                .filter(item -> item != null)
                .map(item -> new ReportEntryItem(
                        item.sku(),
                        item.title(),
                        BigDecimal.valueOf(item.quantity()),
                        money(item.unitValue()),
                        money(item.grossValue())
                ))
                .toList();
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public record ReportEntryItem(
            @JsonProperty("sku") String sku,
            @JsonProperty("title") String title,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("unit_value") BigDecimal unitValue,
            @JsonProperty("gross_value") BigDecimal grossValue) {
    }
}
