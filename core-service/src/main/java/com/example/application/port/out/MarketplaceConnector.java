package com.example.application.port.out;

import com.example.application.command.ConnectorAuthenticationCommand;
import com.example.application.command.ConnectorRefreshTokenCommand;
import com.example.domain.model.connector.ConnectorDescriptor;
import com.example.domain.model.connector.ConnectorStatus;
import com.example.domain.model.connector.ConnectorToken;
import com.example.domain.model.connector.FeeInfo;
import com.example.domain.model.connector.InvoiceFilters;
import com.example.domain.model.connector.InvoiceInfo;
import com.example.domain.model.connector.OrderFilters;
import com.example.domain.model.connector.PaymentInfo;
import com.example.domain.model.connector.StandardOrder;
import com.example.domain.model.connector.SyncResult;

import java.time.Instant;
import java.util.List;

public interface MarketplaceConnector {
    String name();

    ConnectorDescriptor descriptor();

    ConnectorToken authenticate(ConnectorAuthenticationCommand command);

    ConnectorToken refreshToken(ConnectorRefreshTokenCommand command);

    List<StandardOrder> getOrders(String tenantId, OrderFilters filters);

    StandardOrder getOrderDetail(String tenantId, String orderId);

    List<PaymentInfo> getPayments(String tenantId, String orderId);

    List<FeeInfo> getFees(String tenantId, String orderId);

    List<InvoiceInfo> getInvoices(String tenantId, InvoiceFilters filters);

    SyncResult syncAll(String tenantId, Instant since);

    ConnectorStatus getStatus(String tenantId);
}
