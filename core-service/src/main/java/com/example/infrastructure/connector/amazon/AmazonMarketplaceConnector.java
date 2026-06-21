package com.example.infrastructure.connector.amazon;

import com.example.application.command.ConnectorAuthenticationCommand;
import com.example.application.command.ConnectorRefreshTokenCommand;
import com.example.application.exception.ConnectorValidationException;
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
import com.example.domain.model.connector.SyncResult;
import com.example.infrastructure.monitoring.ApiCallContext;
import com.example.infrastructure.monitoring.ApiCallRecorder;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AmazonMarketplaceConnector implements MarketplaceConnector {
    private static final String NAME = "amazon";
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final String MARKETPLACE_ID_BRAZIL = "A2Q3Y263D00KWC";
    private static final int LWA_TOKEN_TTL_SECONDS = 3600;

    @Inject
    AmazonApiClient apiClient;

    @Inject
    JdbcAmazonTokenRepository tokenRepository;

    @Inject
    ApiCallRecorder apiCallRecorder;

    @ConfigProperty(name = "amazon.oauth.client-id", defaultValue = "")
    Optional<String> clientId;

    @ConfigProperty(name = "amazon.oauth.client-secret", defaultValue = "")
    Optional<String> clientSecret;

    @ConfigProperty(name = "amazon.aws.access-key", defaultValue = "")
    Optional<String> awsAccessKey;

    @ConfigProperty(name = "amazon.aws.secret-key", defaultValue = "")
    Optional<String> awsSecretKey;

    @ConfigProperty(name = "amazon.oauth.refresh-skew-seconds", defaultValue = "300")
    long refreshSkewSeconds;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ConnectorDescriptor descriptor() {
        return new ConnectorDescriptor(
                NAME,
                "Amazon",
                false,
                List.of("authenticate", "refreshToken", "getOrders", "getOrderDetail",
                        "getPayments", "getFees", "syncAll", "getStatus"),
                List.of()
        );
    }

    @Override
    public ConnectorToken authenticate(ConnectorAuthenticationCommand command) {
        requireOAuthConfig();
        Map<String, String> credentials = command.credentials() == null ? Map.of() : command.credentials();

        // Two modes: spapi_oauth_code (first auth) or direct refresh_token (re-auth)
        String refreshToken = credentials.get("refresh_token");
        String sellerId = credentials.get("seller_id");

        if (refreshToken == null || refreshToken.isBlank()) {
            String code = requireText(credentials.get("spapi_oauth_code"), "amazon_oauth_code_required");
            String finalCode = code;
            JsonNode response = apiCallRecorder.record(
                    ApiCallContext.of(command.tenantId(), NAME, "/auth/o2/token", "exchange_code"),
                    () -> apiClient.exchangeCode(clientIdValue(), clientSecretValue(), finalCode));
            refreshToken = requireText(text(response, "refresh_token"), "amazon_refresh_token_missing");
        }
        sellerId = requireText(sellerId != null ? sellerId : "", "amazon_seller_id_required");

        // Get initial access token
        String finalRefreshToken = refreshToken;
        JsonNode lwaResponse = apiCallRecorder.record(
                ApiCallContext.of(command.tenantId(), NAME, "/auth/o2/token", "refresh_token"),
                () -> apiClient.refreshToken(clientIdValue(), clientSecretValue(), finalRefreshToken));
        String accessToken = requireText(text(lwaResponse, "access_token"), "amazon_access_token_missing");
        long expiresIn = lwaResponse.path("expires_in").asLong(LWA_TOKEN_TTL_SECONDS);

        AmazonConnectorToken token = new AmazonConnectorToken(
                command.tenantId(), sellerId, accessToken, refreshToken, Instant.now().plusSeconds(expiresIn));
        tokenRepository.save(token);
        return toConnectorToken(token);
    }

    @Override
    public ConnectorToken refreshToken(ConnectorRefreshTokenCommand command) {
        requireOAuthConfig();
        AmazonConnectorToken current = tokenRepository.find(command.tenantId())
                .orElseThrow(() -> new ConnectorValidationException("amazon_not_authenticated"));
        AmazonConnectorToken token = refreshLwa(command.tenantId(), current.sellerId(), current.refreshToken());
        return toConnectorToken(token);
    }

    @Override
    public List<StandardOrder> getOrders(String tenantId, OrderFilters filters) {
        AmazonConnectorToken token = validToken(tenantId);
        OrderFilters applied = filters == null ? new OrderFilters(null, null, null, 50) : filters;
        LocalDate from = applied.from() != null ? applied.from() : LocalDate.now(SAO_PAULO).minusDays(1);
        LocalDate to = applied.to() != null ? applied.to() : LocalDate.now(SAO_PAULO);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("MarketplaceIds", MARKETPLACE_ID_BRAZIL);
        params.put("CreatedAfter", from.atStartOfDay(SAO_PAULO).toInstant().toString());
        params.put("CreatedBefore", to.plusDays(1).atStartOfDay(SAO_PAULO).toInstant().toString());
        params.put("MaxResultsPerPage", String.valueOf(Math.min(applied.limit(), 100)));
        if (applied.status() != null) {
            params.put("OrderStatuses", statusValue(applied.status()));
        }

        JsonNode response = apiCallRecorder.record(
                ApiCallContext.of(tenantId, NAME, "/orders/v0/orders", "get_orders"),
                () -> apiClient.get("/orders/v0/orders", token.accessToken(),
                        awsAccessKeyValue(), awsSecretKeyValue(), params));
        List<StandardOrder> orders = new ArrayList<>();
        for (JsonNode order : response.path("payload").path("Orders")) {
            orders.add(toStandardOrder(order));
        }
        return orders;
    }

    @Override
    public StandardOrder getOrderDetail(String tenantId, String orderId) {
        AmazonConnectorToken token = validToken(tenantId);
        String path = "/orders/v0/orders/" + requireText(orderId, "order_id_required");
        JsonNode response = apiCallRecorder.record(
                ApiCallContext.of(tenantId, NAME, "/orders/v0/orders/{orderId}", "get_order_detail"),
                () -> apiClient.get(path, token.accessToken(), awsAccessKeyValue(), awsSecretKeyValue(), Map.of()));
        return toStandardOrder(response.path("payload"));
    }

    @Override
    public List<PaymentInfo> getPayments(String tenantId, String orderId) {
        AmazonConnectorToken token = validToken(tenantId);
        String path = "/finances/v0/financialEvents/orders/" + requireText(orderId, "order_id_required");
        JsonNode response = apiCallRecorder.record(
                ApiCallContext.of(tenantId, NAME, "/finances/v0/financialEvents/orders/{orderId}", "get_payments"),
                () -> apiClient.get(path, token.accessToken(), awsAccessKeyValue(), awsSecretKeyValue(), Map.of()));
        List<PaymentInfo> payments = new ArrayList<>();
        for (JsonNode event : response.path("payload").path("FinancialEvents").path("ShipmentEventList")) {
            BigDecimal gross = chargeTotal(event, "Principal");
            BigDecimal fee = chargeTotal(event, "FBAPerUnitFulfillmentFee", "Commission").abs();
            payments.add(new PaymentInfo(
                    "PAY-" + orderId,
                    orderId,
                    PaymentMethod.CARD,
                    money(gross),
                    money(gross.subtract(fee)),
                    postedDate(event),
                    postedDate(event),
                    "settled"
            ));
        }
        return payments;
    }

    @Override
    public List<FeeInfo> getFees(String tenantId, String orderId) {
        AmazonConnectorToken token = validToken(tenantId);
        String path = "/finances/v0/financialEvents/orders/" + requireText(orderId, "order_id_required");
        JsonNode response = apiCallRecorder.record(
                ApiCallContext.of(tenantId, NAME, "/finances/v0/financialEvents/orders/{orderId}", "get_fees"),
                () -> apiClient.get(path, token.accessToken(), awsAccessKeyValue(), awsSecretKeyValue(), Map.of()));
        List<FeeInfo> fees = new ArrayList<>();
        for (JsonNode event : response.path("payload").path("FinancialEvents").path("ShipmentEventList")) {
            for (JsonNode feeEvent : event.path("ShipmentFeeList")) {
                String type = text(feeEvent, "FeeType");
                BigDecimal amount = chargeAmount(feeEvent, "FeeAmount");
                if (hasAmount(amount)) {
                    fees.add(new FeeInfo(orderId, standardFeeType(type), "Taxa Amazon: " + type, money(amount)));
                }
            }
        }
        return fees;
    }

    @Override
    public List<InvoiceInfo> getInvoices(String tenantId, InvoiceFilters filters) {
        // Amazon issues invoices separately via the Invoices API (available in some marketplaces).
        // For Brazil, invoice data is embedded in the order. We surface it from recent orders.
        LocalDate from = filters != null && filters.from() != null ? filters.from() : LocalDate.now(SAO_PAULO).minusDays(30);
        LocalDate to = filters != null && filters.to() != null ? filters.to() : LocalDate.now(SAO_PAULO);
        int limit = filters != null ? filters.limit() : 50;
        List<StandardOrder> orders = getOrders(tenantId, new OrderFilters(from, to, OrderStatus.PAID, limit));
        List<InvoiceInfo> invoices = new ArrayList<>();
        for (StandardOrder order : orders) {
            if (order.invoiceNumber() != null && !order.invoiceNumber().isBlank()) {
                invoices.add(new InvoiceInfo(order.invoiceNumber(), order.orderId(), order.date(), "issued", ""));
            }
        }
        return invoices;
    }

    @Override
    public SyncResult syncAll(String tenantId, Instant since) {
        Instant startedAt = Instant.now();
        LocalDate from = since == null ? LocalDate.now(SAO_PAULO).minusDays(1) : since.atZone(SAO_PAULO).toLocalDate();
        int orderCount = getOrders(tenantId, new OrderFilters(from, null, null, 200)).size();
        return new SyncResult(NAME, orderCount, orderCount, orderCount, startedAt, Instant.now());
    }

    @Override
    public ConnectorStatus getStatus(String tenantId) {
        if (!configured()) {
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.DISCONNECTED, "amazon_oauth_not_configured", Instant.now());
        }
        return tokenRepository.find(tenantId)
                .map(token -> statusForToken(tenantId, token))
                .orElseGet(() -> new ConnectorStatus(NAME, ConnectorConnectionStatus.DISCONNECTED, "amazon_not_authenticated", Instant.now()));
    }

    private ConnectorStatus statusForToken(String tenantId, AmazonConnectorToken token) {
        if (token.expiresAt().isBefore(Instant.now())) {
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.EXPIRED, "amazon_token_expired", Instant.now());
        }
        try {
            apiCallRecorder.record(
                    ApiCallContext.of(tenantId, NAME, "/sellers/v1/marketplaceParticipations", "get_status"),
                    () -> apiClient.get("/sellers/v1/marketplaceParticipations", token.accessToken(),
                            awsAccessKeyValue(), awsSecretKeyValue(), Map.of()));
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.ACTIVE, "amazon_connector_active", Instant.now());
        } catch (ConnectorValidationException e) {
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.UNAVAILABLE, e.getMessage(), Instant.now());
        }
    }

    private AmazonConnectorToken validToken(String tenantId) {
        requireOAuthConfig();
        AmazonConnectorToken token = tokenRepository.find(tenantId)
                .orElseThrow(() -> new ConnectorValidationException("amazon_not_authenticated"));
        if (token.expiresAt().minusSeconds(refreshSkewSeconds).isBefore(Instant.now())) {
            return refreshLwa(tenantId, token.sellerId(), token.refreshToken());
        }
        return token;
    }

    private AmazonConnectorToken refreshLwa(String tenantId, String sellerId, String refreshToken) {
        JsonNode response = apiCallRecorder.record(
                ApiCallContext.of(tenantId, NAME, "/auth/o2/token", "refresh_token"),
                () -> apiClient.refreshToken(clientIdValue(), clientSecretValue(), refreshToken));
        String accessToken = requireText(text(response, "access_token"), "amazon_access_token_missing");
        long expiresIn = response.path("expires_in").asLong(LWA_TOKEN_TTL_SECONDS);
        AmazonConnectorToken token = new AmazonConnectorToken(
                tenantId, sellerId, accessToken, refreshToken, Instant.now().plusSeconds(expiresIn));
        tokenRepository.save(token);
        return token;
    }

    private StandardOrder toStandardOrder(JsonNode order) {
        String orderId = text(order, "AmazonOrderId");
        JsonNode orderTotal = order.path("OrderTotal");
        BigDecimal grossValue = decimal(orderTotal, "Amount");
        LocalDate date = parseDate(text(order, "PurchaseDate"));
        LocalDate paymentDate = parseDate(text(order, "LastUpdateDate"));
        return new StandardOrder(
                orderId,
                NAME,
                date != null ? date : LocalDate.now(SAO_PAULO),
                money(grossValue),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                money(grossValue),
                paymentMethod(text(order, "PaymentMethod")),
                paymentDate != null ? paymentDate : date,
                paymentDate != null ? paymentDate.plusDays(14) : null,
                orderStatus(text(order, "OrderStatus")),
                buyerName(order),
                List.of(),
                text(order, "PurchaseOrderNumber")
        );
    }

    private BigDecimal chargeTotal(JsonNode event, String... chargeTypes) {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode charge : event.path("ShipmentItemList")) {
            for (String type : chargeTypes) {
                for (JsonNode item : charge.path("ItemChargeList")) {
                    if (type.equals(text(item, "ChargeType"))) {
                        total = total.add(decimal(item.path("ChargeAmount"), "Amount"));
                    }
                }
            }
        }
        return total;
    }

    private BigDecimal chargeAmount(JsonNode node, String field) {
        return decimal(node.path(field), "Amount");
    }

    private String standardFeeType(String rawType) {
        String type = rawType == null ? "" : rawType.toLowerCase(Locale.ROOT);
        if (type.contains("ship")) {
            return "shipping_cost";
        }
        if (type.contains("commission")) {
            return "commission_fee";
        }
        if (type.contains("fulfillment") || type.contains("fba")) {
            return "fulfillment_fee";
        }
        if (type.contains("closing")) {
            return "closing_fee";
        }
        if (type.contains("service")) {
            return "service_fee";
        }
        return type.isBlank() ? "platform_fee" : type.replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private LocalDate postedDate(JsonNode event) {
        return parseDate(text(event, "PostedDate"));
    }

    private PaymentMethod paymentMethod(String method) {
        if (method == null) return PaymentMethod.UNKNOWN;
        String lower = method.toLowerCase(Locale.ROOT);
        if (lower.contains("credit") || lower.contains("card")) return PaymentMethod.CARD;
        if (lower.contains("boleto")) return PaymentMethod.BOLETO;
        return PaymentMethod.UNKNOWN;
    }

    private OrderStatus orderStatus(String status) {
        if (status == null) return OrderStatus.PENDING;
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "SHIPPED", "DELIVERED" -> OrderStatus.PAID;
            case "CANCELED" -> OrderStatus.CANCELLED;
            default -> OrderStatus.PENDING;
        };
    }

    private String statusValue(OrderStatus status) {
        return switch (status) {
            case PAID -> "Shipped";
            case CANCELLED -> "Canceled";
            case PENDING -> "Pending";
        };
    }

    private String buyerName(JsonNode order) {
        String name = text(order, "BuyerInfo.BuyerName");
        return name.isBlank() ? "Comprador Amazon" : name;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value).atZone(SAO_PAULO).toLocalDate(); }
        catch (DateTimeParseException e) { return null; }
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        if (value == null || value.isMissingNode() || value.isNull()) return BigDecimal.ZERO;
        if (value.isNumber()) return value.decimalValue();
        String t = value.asText("");
        if (t.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(t); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasAmount(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    private String text(JsonNode node, String field) {
        if (node == null) return "";
        JsonNode current = node;
        for (String part : field.split("\\.")) current = current.path(part);
        return current.isMissingNode() || current.isNull() ? "" : current.asText("");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new ConnectorValidationException(message);
        return value.trim();
    }

    private ConnectorToken toConnectorToken(AmazonConnectorToken token) {
        return new ConnectorToken(NAME, ConnectorConnectionStatus.ACTIVE, token.expiresAt());
    }

    private boolean configured() {
        return !clientIdValue().isBlank() && !clientSecretValue().isBlank() && !awsAccessKeyValue().isBlank();
    }

    private void requireOAuthConfig() {
        if (!configured()) throw new ConnectorValidationException("amazon_oauth_not_configured");
    }

    private String clientIdValue() { return clientId == null ? "" : clientId.orElse("").trim(); }
    private String clientSecretValue() { return clientSecret == null ? "" : clientSecret.orElse("").trim(); }
    private String awsAccessKeyValue() { return awsAccessKey == null ? "" : awsAccessKey.orElse("").trim(); }
    private String awsSecretKeyValue() { return awsSecretKey == null ? "" : awsSecretKey.orElse("").trim(); }
}
