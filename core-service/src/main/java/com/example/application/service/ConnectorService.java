package com.example.application.service;

import com.example.application.command.ConnectorAuthenticationCommand;
import com.example.application.command.ConnectorRefreshTokenCommand;
import com.example.application.exception.ConnectorNotFoundException;
import com.example.application.exception.ConnectorValidationException;
import com.example.application.event.NewSaleEvent;
import com.example.application.port.out.ConnectorRegistry;
import com.example.application.port.out.DomainEventPublisher;
import com.example.application.port.out.MarketplaceConnector;
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
import com.example.domain.model.connector.SyncResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ConnectorService {
    private final ConnectorRegistry connectorRegistry;
    private final DomainEventPublisher domainEventPublisher;

    @Inject
    public ConnectorService(ConnectorRegistry connectorRegistry, DomainEventPublisher domainEventPublisher) {
        this.connectorRegistry = connectorRegistry;
        this.domainEventPublisher = domainEventPublisher;
    }

    public List<ConnectorDescriptor> list() {
        return connectorRegistry.list().stream()
                .map(MarketplaceConnector::descriptor)
                .toList();
    }

    public ConnectorToken authenticate(String connectorName, String tenantId, Map<String, String> credentials) {
        return connector(connectorName).authenticate(new ConnectorAuthenticationCommand(
                connectorName,
                requireText(tenantId, "tenantId"),
                credentials
        ));
    }

    public ConnectorToken refreshToken(String connectorName, String tenantId) {
        return connector(connectorName).refreshToken(new ConnectorRefreshTokenCommand(
                connectorName,
                requireText(tenantId, "tenantId")
        ));
    }

    public List<StandardOrder> getOrders(String connectorName, String tenantId, LocalDate from, LocalDate to, OrderStatus status, Integer limit) {
        return connector(connectorName).getOrders(
                requireText(tenantId, "tenantId"),
                new OrderFilters(from, to, status, limit == null ? 50 : limit)
        );
    }

    public StandardOrder getOrderDetail(String connectorName, String tenantId, String orderId) {
        return connector(connectorName).getOrderDetail(requireText(tenantId, "tenantId"), requireText(orderId, "orderId"));
    }

    public List<PaymentInfo> getPayments(String connectorName, String tenantId, String orderId) {
        return connector(connectorName).getPayments(requireText(tenantId, "tenantId"), requireText(orderId, "orderId"));
    }

    public List<FeeInfo> getFees(String connectorName, String tenantId, String orderId) {
        return connector(connectorName).getFees(requireText(tenantId, "tenantId"), requireText(orderId, "orderId"));
    }

    public List<InvoiceInfo> getInvoices(String connectorName, String tenantId, LocalDate from, LocalDate to, Integer limit) {
        return connector(connectorName).getInvoices(
                requireText(tenantId, "tenantId"),
                new InvoiceFilters(from, to, limit == null ? 50 : limit)
        );
    }

    public SyncResult syncAll(String connectorName, String tenantId, String recipientEmail, Instant since) {
        String normalizedTenantId = requireText(tenantId, "tenantId");
        MarketplaceConnector marketplaceConnector = connector(connectorName);
        SyncResult result = marketplaceConnector.syncAll(normalizedTenantId, since == null ? Instant.now().minusSeconds(86400) : since);
        publishNewSaleEvents(marketplaceConnector, normalizedTenantId, recipientEmail);
        return result;
    }

    public ConnectorStatus getStatus(String connectorName, String tenantId) {
        return connector(connectorName).getStatus(requireText(tenantId, "tenantId"));
    }

    private MarketplaceConnector connector(String connectorName) {
        String normalizedName = requireText(connectorName, "connectorName").toLowerCase();
        return connectorRegistry.find(normalizedName)
                .orElseThrow(() -> new ConnectorNotFoundException(normalizedName));
    }

    private void publishNewSaleEvents(MarketplaceConnector marketplaceConnector, String tenantId, String recipientEmail) {
        marketplaceConnector.getOrders(tenantId, new OrderFilters(null, null, OrderStatus.PAID, 50)).stream()
                .filter(order -> order.status() == OrderStatus.PAID)
                .forEach(order -> domainEventPublisher.publishNewSale(NewSaleEvent.create(
                        UUID.randomUUID().toString(),
                        tenantId,
                        recipientEmail,
                        order.platform(),
                        order.orderId(),
                        order.grossValue()
                )));
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ConnectorValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
