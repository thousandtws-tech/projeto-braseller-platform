package com.example.infrastructure.connector;

import com.example.application.command.ConnectorAuthenticationCommand;
import com.example.application.command.ConnectorRefreshTokenCommand;
import com.example.application.port.out.MarketplaceConnector;
import com.example.domain.enums.PaymentMethod;
import com.example.domain.model.connector.ConnectorConnectionStatus;
import com.example.domain.model.connector.ConnectorDescriptor;
import com.example.domain.model.connector.ConnectorStatus;
import com.example.domain.model.connector.ConnectorToken;
import com.example.domain.model.connector.FeeInfo;
import com.example.domain.model.connector.InvoiceFilters;
import com.example.domain.model.connector.InvoiceInfo;
import com.example.domain.model.connector.OrderFilters;
import com.example.domain.model.connector.OrderStatus;
import com.example.domain.model.connector.PaymentInfo;
import com.example.domain.model.connector.StandardOrder;
import com.example.domain.model.connector.StandardOrderItem;
import com.example.domain.model.connector.SyncResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SandboxMarketplaceConnector implements MarketplaceConnector {
    private static final String NAME = "sandbox";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ConnectorDescriptor descriptor() {
        return new ConnectorDescriptor(
                NAME,
                "Sandbox Connector",
                true,
                List.of(
                        "authenticate",
                        "refreshToken",
                        "getOrders",
                        "getOrderDetail",
                        "getPayments",
                        "getFees",
                        "syncAll",
                        "getStatus"
                ),
                List.of("getInvoices")
        );
    }

    @Override
    public ConnectorToken authenticate(ConnectorAuthenticationCommand command) {
        return token("sandbox-access-" + command.tenantId(), "sandbox-refresh-" + UUID.randomUUID());
    }

    @Override
    public ConnectorToken refreshToken(ConnectorRefreshTokenCommand command) {
        return token();
    }

    @Override
    public List<StandardOrder> getOrders(String tenantId, OrderFilters filters) {
        OrderFilters appliedFilters = filters == null ? new OrderFilters(null, null, null, 50) : filters;
        return allOrders().stream()
                .filter(order -> matches(order, appliedFilters))
                .limit(appliedFilters.limit())
                .toList();
    }

    @Override
    public StandardOrder getOrderDetail(String tenantId, String orderId) {
        return order(orderId);
    }

    @Override
    public List<PaymentInfo> getPayments(String tenantId, String orderId) {
        StandardOrder order = order(orderId);
        return List.of(new PaymentInfo(
                "PAY-" + orderId,
                orderId,
                order.paymentMethod(),
                order.grossValue(),
                order.netValue(),
                order.paymentDate(),
                order.releaseDate(),
                switch (order.status()) {
                    case PAID -> "approved";
                    case PENDING -> "scheduled";
                    case CANCELLED -> "cancelled";
                }
        ));
    }

    @Override
    public List<FeeInfo> getFees(String tenantId, String orderId) {
        StandardOrder order = order(orderId);
        BigDecimal commission = order.grossValue().multiply(new BigDecimal("0.10"));
        BigDecimal shipping = order.grossValue().compareTo(new BigDecimal("100.00")) >= 0
                ? new BigDecimal("7.50")
                : new BigDecimal("4.90");
        return List.of(
                new FeeInfo(orderId, "platform_fee", "Taxa de venda da plataforma", commission),
                new FeeInfo(orderId, "shipping_cost", "Repasse logistico", shipping)
        );
    }

    @Override
    public List<InvoiceInfo> getInvoices(String tenantId, InvoiceFilters filters) {
        InvoiceFilters appliedFilters = filters == null
                ? new InvoiceFilters(null, null, 50)
                : filters;
        return allOrders().stream()
                .filter(order -> order.status() == OrderStatus.PAID)
                .filter(order -> appliedFilters.from() == null || !order.date().isBefore(appliedFilters.from()))
                .filter(order -> appliedFilters.to() == null || !order.date().isAfter(appliedFilters.to()))
                .limit(appliedFilters.limit())
                .map(order -> new InvoiceInfo(
                        "NF-" + order.orderId(),
                        order.orderId(),
                        order.date(),
                        "issued",
                        "00000000000000000000000000000000000000000000"
                ))
                .toList();
    }

    @Override
    public SyncResult syncAll(String tenantId, Instant since) {
        Instant startedAt = Instant.now();
        int orderCount = (int) allOrders().stream()
                .filter(order -> since == null || !order.date().atStartOfDay().toInstant(java.time.ZoneOffset.UTC).isBefore(since))
                .count();
        return new SyncResult(NAME, orderCount, orderCount, orderCount, startedAt, Instant.now());
    }

    @Override
    public ConnectorStatus getStatus(String tenantId) {
        return new ConnectorStatus(NAME, ConnectorConnectionStatus.ACTIVE, "sandbox_connector_active", Instant.now());
    }

    private ConnectorToken token(String accessToken, String refreshToken) {
        return token();
    }

    private ConnectorToken token() {
        return new ConnectorToken(NAME, ConnectorConnectionStatus.ACTIVE, Instant.now().plusSeconds(3600));
    }

    private List<StandardOrder> allOrders() {
        LocalDate today = LocalDate.now();
        return List.of(
                order("SANDBOX-1001", today.minusDays(1), "199.90", PaymentMethod.PIX, OrderStatus.PAID, "Cliente Teste PIX"),
                order("SANDBOX-1002", today.minusDays(2), "349.00", PaymentMethod.CARD, OrderStatus.PAID, "Cliente Teste Cartao"),
                order("SANDBOX-1003", today.minusDays(3), "89.90", PaymentMethod.BOLETO, OrderStatus.PENDING, "Cliente Teste Boleto"),
                order("SANDBOX-1004", today.minusDays(4), "599.00", PaymentMethod.MARKETPLACE_BALANCE, OrderStatus.PAID, "Cliente Teste Saldo"),
                order("SANDBOX-1005", today.minusDays(5), "129.50", PaymentMethod.CARD, OrderStatus.CANCELLED, "Cliente Teste Cancelado"),
                order("SANDBOX-1006", today.minusDays(6), "54.90", PaymentMethod.PIX, OrderStatus.PAID, "Cliente Teste Recorrente")
        );
    }

    private boolean matches(StandardOrder order, OrderFilters filters) {
        if (filters.status() != null && order.status() != filters.status()) {
            return false;
        }
        if (filters.from() != null && order.date().isBefore(filters.from())) {
            return false;
        }
        return filters.to() == null || !order.date().isAfter(filters.to());
    }

    private StandardOrder order(String orderId) {
        return allOrders().stream()
                .filter(order -> order.orderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("sandbox_order_not_found: " + orderId));
    }

    private StandardOrder order(
            String orderId,
            LocalDate saleDate,
            String grossAmount,
            PaymentMethod paymentMethod,
            OrderStatus status,
            String buyerName) {
        BigDecimal grossValue = new BigDecimal(grossAmount);
        BigDecimal platformFee = grossValue.multiply(new BigDecimal("0.10")).add(
                grossValue.compareTo(new BigDecimal("100.00")) >= 0
                        ? new BigDecimal("7.50")
                        : new BigDecimal("4.90")
        );
        BigDecimal netValue = grossValue.subtract(platformFee);
        return new StandardOrder(
                orderId,
                NAME,
                saleDate,
                grossValue,
                platformFee,
                netValue,
                paymentMethod,
                status == OrderStatus.PENDING ? null : saleDate,
                saleDate.plusDays(14),
                status,
                buyerName,
                List.of(new StandardOrderItem(
                        "SKU-" + orderId.substring(orderId.length() - 4),
                        "Produto de Teste " + orderId.substring(orderId.length() - 1),
                        1,
                        grossValue,
                        grossValue
                )),
                status == OrderStatus.PAID ? "NF-" + orderId : null
        );
    }
}
