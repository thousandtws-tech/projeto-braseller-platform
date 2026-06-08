package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public record FeeInfo(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("type") String type,
        @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount) {

    public static final String COMMISSION_FEE = "commission_fee";
    public static final String SERVICE_FEE = "service_fee";
    public static final String TRANSACTION_FEE = "transaction_fee";
    public static final String FULFILLMENT_FEE = "fulfillment_fee";
    public static final String CLOSING_FEE = "closing_fee";
    public static final String SHIPPING_COST = "shipping_cost";
    public static final String PLATFORM_FEE = "platform_fee";

    public FeeInfo {
        type = normalizeType(type);
        description = description == null ? "" : description;
        amount = normalizeAmount(amount);
    }

    public boolean isShippingCost() {
        return SHIPPING_COST.equals(type);
    }

    public boolean hasAmount() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static String normalizeType(String rawType) {
        String normalized = rawType == null ? "" : rawType
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");

        if (normalized.isBlank()) {
            return PLATFORM_FEE;
        }
        if (normalized.contains("ship") || normalized.contains("frete") || normalized.contains("logistic")) {
            return SHIPPING_COST;
        }
        if (normalized.equals("sale_fee") || normalized.contains("commission") || normalized.contains("comissao")) {
            return COMMISSION_FEE;
        }
        if (normalized.contains("transaction") || normalized.contains("seller_transaction")) {
            return TRANSACTION_FEE;
        }
        if (normalized.contains("fulfillment") || normalized.contains("fba")) {
            return FULFILLMENT_FEE;
        }
        if (normalized.contains("closing")) {
            return CLOSING_FEE;
        }
        if (normalized.contains("service") || normalized.contains("servico")) {
            return SERVICE_FEE;
        }
        return normalized;
    }

    private static BigDecimal normalizeAmount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value.abs()).setScale(2, RoundingMode.HALF_UP);
    }
}
