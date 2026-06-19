package com.example.application.service;

import com.example.application.command.ConnectorAuthenticationCommand;
import com.example.application.command.ConnectorRefreshTokenCommand;
import com.example.application.exception.ConnectorNotFoundException;
import com.example.application.exception.ConnectorValidationException;
import com.example.application.exception.NotFoundException;
import com.example.application.event.NewSaleEvent;
import com.example.application.event.ReportEntryUpsertRequestedEvent;
import com.example.application.event.SyncAllRequestedEvent;
import com.example.application.port.out.ConnectorSyncJobRepository;
import com.example.application.port.out.ConnectorSyncQueue;
import com.example.application.port.out.ConnectorRegistry;
import com.example.application.port.out.DomainEventPublisher;
import com.example.application.port.out.MarketplaceConnector;
import com.example.application.port.out.ReportEntryEventPublisher;
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
import com.example.domain.model.connector.SyncAccepted;
import com.example.domain.model.connector.SyncJob;
import com.example.domain.model.connector.SyncResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ConnectorService {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final int DEFAULT_SYNC_LOOKBACK_SECONDS = 86400;
    private static final int DEFAULT_SYNC_ORDER_LIMIT = 200;

    private final ConnectorRegistry connectorRegistry;
    private final DomainEventPublisher domainEventPublisher;
    private final ConnectorSyncQueue connectorSyncQueue;
    private final ConnectorSyncJobRepository connectorSyncJobRepository;
    private final ReportEntryEventPublisher reportEntryEventPublisher;
    private final ConnectorRealtimeService connectorRealtimeService;

    @Inject
    public ConnectorService(
            ConnectorRegistry connectorRegistry,
            DomainEventPublisher domainEventPublisher,
            ConnectorSyncQueue connectorSyncQueue,
            ConnectorSyncJobRepository connectorSyncJobRepository,
            ReportEntryEventPublisher reportEntryEventPublisher,
            ConnectorRealtimeService connectorRealtimeService) {
        this.connectorRegistry = connectorRegistry;
        this.domainEventPublisher = domainEventPublisher;
        this.connectorSyncQueue = connectorSyncQueue;
        this.connectorSyncJobRepository = connectorSyncJobRepository;
        this.reportEntryEventPublisher = reportEntryEventPublisher;
        this.connectorRealtimeService = connectorRealtimeService;
    }

    public List<ConnectorDescriptor> list() {
        return connectorRegistry.list().stream()
                .map(MarketplaceConnector::descriptor)
                .toList();
    }

    public ConnectorToken authenticate(String connectorName, String tenantId, Map<String, String> credentials) {
        String normalizedTenantId = requireText(tenantId, "tenantId");
        ConnectorToken token = connector(connectorName).authenticate(new ConnectorAuthenticationCommand(
                connectorName,
                normalizedTenantId,
                credentials
        ));
        connectorRealtimeService.connectorAuthenticated(normalizedTenantId, token);
        return token;
    }

    public ConnectorToken refreshToken(String connectorName, String tenantId) {
        String normalizedTenantId = requireText(tenantId, "tenantId");
        ConnectorToken token = connector(connectorName).refreshToken(new ConnectorRefreshTokenCommand(
                connectorName,
                normalizedTenantId
        ));
        connectorRealtimeService.connectorTokenRefreshed(normalizedTenantId, token);
        return token;
    }

    public List<StandardOrder> getOrders(String connectorName, String tenantId, LocalDate from, LocalDate to, OrderStatus status, Integer limit) {
        MarketplaceConnector marketplaceConnector = connector(connectorName);
        String normalizedTenantId = requireText(tenantId, "tenantId");
        return marketplaceConnector.getOrders(
                normalizedTenantId,
                new OrderFilters(from, to, status, limit == null ? 50 : limit)
        ).stream()
                .map(order -> withFeeSplit(marketplaceConnector, normalizedTenantId, order))
                .toList();
    }

    public StandardOrder getOrderDetail(String connectorName, String tenantId, String orderId) {
        MarketplaceConnector marketplaceConnector = connector(connectorName);
        String normalizedTenantId = requireText(tenantId, "tenantId");
        return withFeeSplit(
                marketplaceConnector,
                normalizedTenantId,
                marketplaceConnector.getOrderDetail(normalizedTenantId, requireText(orderId, "orderId"))
        );
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
        return performSyncAll(connectorName, tenantId, recipientEmail, since);
    }

    public SyncAccepted requestSyncAll(String connectorName, String tenantId, String recipientEmail, Instant since) {
        MarketplaceConnector marketplaceConnector = connector(connectorName);
        String normalizedTenantId = requireText(tenantId, "tenantId");
        Instant resolvedSince = resolveSince(since);
        SyncAllRequestedEvent event = SyncAllRequestedEvent.create(
                marketplaceConnector.name(),
                normalizedTenantId,
                recipientEmail,
                resolvedSince
        );
        connectorSyncJobRepository.createQueued(event);
        connectorSyncQueue.enqueue(event);
        return new SyncAccepted(
                event.eventId(),
                "QUEUED",
                event.connectorName(),
                event.tenantId(),
                event.since(),
                event.requestedAt()
        );
    }

    public SyncResult processSyncAll(SyncAllRequestedEvent event) {
        if (event == null) {
            throw new ConnectorValidationException("sync event is required");
        }
        if (!connectorSyncJobRepository.tryMarkProcessing(event.eventId())) {
            return null;
        }
        try {
            SyncResult result = performSyncAll(event.connectorName(), event.tenantId(), event.recipientEmail(), event.since());
            connectorSyncJobRepository.markCompleted(event.eventId(), result);
            return result;
        } catch (RuntimeException exception) {
            connectorSyncJobRepository.markFailed(event.eventId(), exception.getMessage());
            throw exception;
        }
    }

    public SyncJob syncJob(String tenantId, String jobId) {
        return connectorSyncJobRepository.find(requireText(tenantId, "tenantId"), requireText(jobId, "jobId"))
                .orElseThrow(() -> new NotFoundException("sync_job_not_found"));
    }

    public ConnectorStatus getStatus(String connectorName, String tenantId) {
        return connector(connectorName).getStatus(requireText(tenantId, "tenantId"));
    }

    private MarketplaceConnector connector(String connectorName) {
        String normalizedName = requireText(connectorName, "connectorName").toLowerCase();
        return connectorRegistry.find(normalizedName)
                .orElseThrow(() -> new ConnectorNotFoundException(normalizedName));
    }

    private SyncResult performSyncAll(String connectorName, String tenantId, String recipientEmail, Instant since) {
        String normalizedTenantId = requireText(tenantId, "tenantId");
        MarketplaceConnector marketplaceConnector = connector(connectorName);
        Instant resolvedSince = resolveSince(since);
        SyncResult result = marketplaceConnector.syncAll(normalizedTenantId, resolvedSince);
        List<StandardOrder> syncedOrders = marketplaceConnector.getOrders(
                normalizedTenantId,
                new OrderFilters(
                        resolvedSince.atZone(SAO_PAULO).toLocalDate(),
                        null,
                        null,
                        Math.max(DEFAULT_SYNC_ORDER_LIMIT, result.ordersSynced())
                )
        ).stream()
                .map(order -> withFeeSplit(marketplaceConnector, normalizedTenantId, order))
                .toList();
        publishReportEntryEvents(normalizedTenantId, syncedOrders);
        publishNewSaleEvents(normalizedTenantId, recipientEmail, syncedOrders);
        return result;
    }

    private void publishReportEntryEvents(String tenantId, List<StandardOrder> orders) {
        orders.forEach(order -> reportEntryEventPublisher.publishReportEntryUpsert(
                ReportEntryUpsertRequestedEvent.fromOrder(tenantId, order)
        ));
    }

    private void publishNewSaleEvents(String tenantId, String recipientEmail, List<StandardOrder> orders) {
        orders.stream()
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

    private StandardOrder withFeeSplit(MarketplaceConnector connector, String tenantId, StandardOrder order) {
        if (order == null) {
            return null;
        }
        BigDecimal feeValue = effectiveFeeValue(order, connector.getFees(tenantId, order.orderId()));
        BigDecimal grossValue = money(order.grossValue());
        return new StandardOrder(
                order.orderId(),
                order.platform(),
                order.date(),
                grossValue,
                feeValue,
                grossValue.subtract(feeValue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                order.paymentMethod(),
                order.paymentDate(),
                order.releaseDate(),
                order.status(),
                order.buyerName(),
                order.items(),
                order.invoiceNumber()
        );
    }

    private BigDecimal effectiveFeeValue(StandardOrder order, List<FeeInfo> fees) {
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

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Instant resolveSince(Instant since) {
        return since == null ? Instant.now().minusSeconds(DEFAULT_SYNC_LOOKBACK_SECONDS) : since;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ConnectorValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
