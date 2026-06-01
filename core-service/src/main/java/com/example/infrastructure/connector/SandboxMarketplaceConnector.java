package com.example.infrastructure.connector;

import com.example.application.command.ConnectorAuthenticationCommand;
import com.example.application.command.ConnectorRefreshTokenCommand;
import com.example.application.port.out.MarketplaceConnector;
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
import com.example.domain.model.connector.PaymentMethod;
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
                "scheduled"
        ));
    }

    @Override
    public List<FeeInfo> getFees(String tenantId, String orderId) {
        return List.of(
                new FeeInfo(orderId, "platform_fee", "Taxa de venda da plataforma", new BigDecimal("18.90")),
                new FeeInfo(orderId, "shipping_fee", "Repasse logistico", new BigDecimal("7.50"))
        );
    }

    @Override
    public List<InvoiceInfo> getInvoices(String tenantId, InvoiceFilters filters) {
        return List.of(new InvoiceInfo(
                "NF-SANDBOX-1001",
                "SANDBOX-1001",
                LocalDate.now().minusDays(1),
                "issued",
                "00000000000000000000000000000000000000000000"
        ));
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
        return List.of(order("SANDBOX-1001"), order("SANDBOX-1002"));
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
        BigDecimal grossValue = new BigDecimal("199.90");
        BigDecimal platformFee = new BigDecimal("26.40");
        BigDecimal netValue = grossValue.subtract(platformFee);
        LocalDate saleDate = LocalDate.now().minusDays(2);
        return new StandardOrder(
                orderId,
                NAME,
                saleDate,
                grossValue,
                platformFee,
                netValue,
                PaymentMethod.PIX,
                saleDate,
                saleDate.plusDays(14),
                OrderStatus.PAID,
                "Comprador Sandbox",
                List.of(new StandardOrderItem("SKU-001", "Produto Sandbox", 1, grossValue, grossValue)),
                "NF-" + orderId
        );
    }
}
