package com.example.infrastructure.connector.mercadolivre;

import com.example.application.command.ConnectorAuthenticationCommand;
import com.example.application.command.ConnectorRefreshTokenCommand;
import com.example.application.exception.ConnectorValidationException;
import com.example.application.port.out.MarketplaceConnector;
import com.example.domain.model.connector.ConnectorConnectionStatus;
import com.example.domain.model.connector.ConnectorDescriptor;
import com.example.domain.model.connector.ConnectorStatus;
import com.example.domain.model.connector.ConnectorToken;
import com.example.domain.model.connector.FeeInfo;
import com.example.domain.model.connector.OrderFilters;
import com.example.domain.model.connector.OrderStatus;
import com.example.domain.model.connector.PaymentInfo;
import com.example.domain.model.connector.PaymentMethod;
import com.example.domain.model.connector.StandardOrder;
import com.example.domain.model.connector.StandardOrderItem;
import com.example.domain.model.connector.SyncResult;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class MercadoLivreMarketplaceConnector implements MarketplaceConnector {
    private static final String NAME = "mercado-livre";
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final int DEFAULT_TOKEN_TTL_SECONDS = 21600;

    @Inject
    MercadoLivreApiClient apiClient;

    @Inject
    JdbcMercadoLivreTokenRepository tokenRepository;

    @ConfigProperty(name = "mercadolivre.oauth.client-id", defaultValue = "")
    Optional<String> clientId;

    @ConfigProperty(name = "mercadolivre.oauth.client-secret", defaultValue = "")
    Optional<String> clientSecret;

    @ConfigProperty(name = "mercadolivre.oauth.redirect-uri", defaultValue = "")
    Optional<String> redirectUri;

    @ConfigProperty(name = "mercadolivre.oauth.refresh-skew-seconds", defaultValue = "300")
    long refreshSkewSeconds;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ConnectorDescriptor descriptor() {
        return new ConnectorDescriptor(
                NAME,
                "Mercado Livre",
                false,
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
                List.of()
        );
    }

    @Override
    public ConnectorToken authenticate(ConnectorAuthenticationCommand command) {
        requireOAuthConfig();
        Map<String, String> credentials = command.credentials() == null ? Map.of() : command.credentials();
        String code = requireText(credentials.get("code"), "mercado_livre_oauth_code_required");
        String callbackUri = textOrDefault(credentials.get("redirect_uri"), configValue(redirectUri));
        if (callbackUri.isBlank()) {
            throw new ConnectorValidationException("mercado_livre_redirect_uri_required");
        }
        JsonNode response = apiClient.exchangeCode(configValue(clientId), configValue(clientSecret), callbackUri, code);
        MercadoLivreConnectorToken token = tokenFromResponse(command.tenantId(), null, response);
        tokenRepository.save(token);
        return toConnectorToken(token);
    }

    @Override
    public ConnectorToken refreshToken(ConnectorRefreshTokenCommand command) {
        requireOAuthConfig();
        String refreshToken = requireText(command.refreshToken(), "mercado_livre_refresh_token_required");
        MercadoLivreConnectorToken token = refresh(command.tenantId(), null, refreshToken);
        return toConnectorToken(token);
    }

    @Override
    public List<StandardOrder> getOrders(String tenantId, OrderFilters filters) {
        MercadoLivreConnectorToken token = validToken(tenantId);
        OrderFilters appliedFilters = filters == null ? new OrderFilters(null, null, null, 50) : filters;
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("seller", token.sellerId());
        queryParameters.put("limit", String.valueOf(appliedFilters.limit()));
        if (appliedFilters.status() != null) {
            queryParameters.put("order.status", statusValue(appliedFilters.status()));
        }
        if (appliedFilters.from() != null) {
            queryParameters.put("order.date_created.from", startOfDayUtc(appliedFilters.from()));
        }
        if (appliedFilters.to() != null) {
            queryParameters.put("order.date_created.to", endOfDayUtc(appliedFilters.to()));
        }
        JsonNode response = apiClient.get("/orders/search", token.accessToken(), queryParameters);
        List<StandardOrder> orders = new ArrayList<>();
        for (JsonNode order : response.path("results")) {
            orders.add(toStandardOrder(order));
        }
        return orders;
    }

    @Override
    public StandardOrder getOrderDetail(String tenantId, String orderId) {
        MercadoLivreConnectorToken token = validToken(tenantId);
        JsonNode order = apiClient.get("/orders/" + requireText(orderId, "order_id_required"), token.accessToken());
        return toStandardOrder(order);
    }

    @Override
    public List<PaymentInfo> getPayments(String tenantId, String orderId) {
        MercadoLivreConnectorToken token = validToken(tenantId);
        JsonNode order = apiClient.get("/orders/" + requireText(orderId, "order_id_required"), token.accessToken());
        List<PaymentInfo> payments = new ArrayList<>();
        for (JsonNode payment : order.path("payments")) {
            String paymentId = text(payment, "id");
            JsonNode paymentDetails = payment;
            if (!paymentId.isBlank()) {
                paymentDetails = apiClient.get("/payments/" + paymentId, token.accessToken());
            }
            payments.add(toPaymentInfo(text(order, "id"), paymentDetails));
        }
        return payments;
    }

    @Override
    public List<FeeInfo> getFees(String tenantId, String orderId) {
        MercadoLivreConnectorToken token = validToken(tenantId);
        JsonNode order = apiClient.get("/orders/" + requireText(orderId, "order_id_required"), token.accessToken());
        List<FeeInfo> fees = new ArrayList<>();
        BigDecimal saleFee = saleFee(order);
        BigDecimal shippingCost = shippingCost(order);
        if (isPositive(saleFee)) {
            fees.add(new FeeInfo(text(order, "id"), "sale_fee", "Tarifa de venda Mercado Livre", money(saleFee)));
        }
        if (isPositive(shippingCost)) {
            fees.add(new FeeInfo(text(order, "id"), "shipping_cost", "Custo de envio Mercado Livre", money(shippingCost)));
        }
        return fees;
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
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.DISCONNECTED, "mercado_livre_oauth_not_configured", Instant.now());
        }
        return tokenRepository.find(tenantId)
                .map(this::statusForToken)
                .orElseGet(() -> new ConnectorStatus(NAME, ConnectorConnectionStatus.DISCONNECTED, "mercado_livre_not_authenticated", Instant.now()));
    }

    private ConnectorStatus statusForToken(MercadoLivreConnectorToken token) {
        if (token.expiresAt().isBefore(Instant.now())) {
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.EXPIRED, "mercado_livre_token_expired", Instant.now());
        }
        try {
            apiClient.get("/users/" + token.sellerId(), token.accessToken());
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.ACTIVE, "mercado_livre_connector_active", Instant.now());
        } catch (ConnectorValidationException exception) {
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.UNAVAILABLE, exception.getMessage(), Instant.now());
        }
    }

    private MercadoLivreConnectorToken validToken(String tenantId) {
        requireOAuthConfig();
        MercadoLivreConnectorToken token = tokenRepository.find(tenantId)
                .orElseThrow(() -> new ConnectorValidationException("mercado_livre_not_authenticated"));
        if (token.sellerId() == null || token.sellerId().isBlank()) {
            throw new ConnectorValidationException("mercado_livre_seller_id_required");
        }
        if (token.expiresAt().minusSeconds(refreshSkewSeconds).isBefore(Instant.now())) {
            return refresh(tenantId, token.sellerId(), token.refreshToken());
        }
        return token;
    }

    private MercadoLivreConnectorToken refresh(String tenantId, String knownSellerId, String refreshToken) {
        JsonNode response = apiClient.refreshToken(configValue(clientId), configValue(clientSecret), refreshToken);
        MercadoLivreConnectorToken token = tokenFromResponse(tenantId, knownSellerId, response);
        tokenRepository.save(token);
        return token;
    }

    private MercadoLivreConnectorToken tokenFromResponse(String tenantId, String knownSellerId, JsonNode response) {
        String accessToken = requireText(text(response, "access_token"), "mercado_livre_access_token_missing");
        String refreshToken = requireText(text(response, "refresh_token"), "mercado_livre_refresh_token_missing");
        String sellerId = text(response, "user_id");
        if (sellerId.isBlank()) {
            sellerId = knownSellerId;
        }
        if (sellerId == null || sellerId.isBlank()) {
            throw new ConnectorValidationException("mercado_livre_seller_id_missing");
        }
        long expiresIn = response.path("expires_in").asLong(DEFAULT_TOKEN_TTL_SECONDS);
        return new MercadoLivreConnectorToken(tenantId, sellerId, accessToken, refreshToken, Instant.now().plusSeconds(expiresIn));
    }

    private ConnectorToken toConnectorToken(MercadoLivreConnectorToken token) {
        return new ConnectorToken(NAME, token.accessToken(), token.refreshToken(), token.expiresAt());
    }

    private StandardOrder toStandardOrder(JsonNode order) {
        String orderId = text(order, "id");
        BigDecimal grossValue = firstAmount(order, "total_amount", "paid_amount");
        if (!isPositive(grossValue)) {
            grossValue = itemsGrossValue(order);
        }
        BigDecimal platformFee = saleFee(order).add(shippingCost(order));
        LocalDate saleDate = date(order, "date_created", "date_closed");
        LocalDate paymentDate = paymentDate(order);
        LocalDate releaseDate = releaseDate(order);
        return new StandardOrder(
                orderId,
                NAME,
                saleDate,
                money(grossValue),
                money(platformFee),
                money(grossValue.subtract(platformFee)),
                paymentMethod(firstPayment(order)),
                paymentDate,
                releaseDate,
                orderStatus(text(order, "status")),
                buyerName(order),
                items(order),
                null
        );
    }

    private PaymentInfo toPaymentInfo(String orderId, JsonNode payment) {
        BigDecimal grossValue = firstAmount(payment, "transaction_amount", "total_paid_amount", "amount");
        BigDecimal netValue = firstAmount(payment, "net_received_amount", "transaction_details.net_received_amount");
        if (!isPositive(netValue)) {
            netValue = grossValue.subtract(firstAmount(payment, "marketplace_fee"));
        }
        return new PaymentInfo(
                text(payment, "id"),
                orderId,
                paymentMethod(payment),
                money(grossValue),
                money(netValue),
                date(payment, "date_approved", "date_created"),
                date(payment, "money_release_date", "available_date"),
                textOrDefault(text(payment, "status"), "unknown")
        );
    }

    private List<StandardOrderItem> items(JsonNode order) {
        List<StandardOrderItem> items = new ArrayList<>();
        for (JsonNode item : order.path("order_items")) {
            JsonNode itemInfo = item.path("item");
            int quantity = item.path("quantity").asInt(1);
            BigDecimal unitValue = firstAmount(item, "unit_price", "full_unit_price");
            items.add(new StandardOrderItem(
                    textOrDefault(text(itemInfo, "seller_sku"), text(itemInfo, "id")),
                    text(itemInfo, "title"),
                    quantity,
                    money(unitValue),
                    money(unitValue.multiply(BigDecimal.valueOf(quantity)))
            ));
        }
        return items;
    }

    private BigDecimal itemsGrossValue(JsonNode order) {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode item : order.path("order_items")) {
            int quantity = item.path("quantity").asInt(1);
            total = total.add(firstAmount(item, "unit_price", "full_unit_price").multiply(BigDecimal.valueOf(quantity)));
        }
        return total;
    }

    private BigDecimal saleFee(JsonNode order) {
        BigDecimal total = firstAmount(order, "sale_fee", "marketplace_fee");
        for (JsonNode item : order.path("order_items")) {
            int quantity = item.path("quantity").asInt(1);
            total = total.add(firstAmount(item, "sale_fee").multiply(BigDecimal.valueOf(quantity)));
        }
        return total;
    }

    private BigDecimal shippingCost(JsonNode order) {
        return firstAmount(order, "shipping_cost", "shipping.seller.cost", "shipping.seller_cost", "shipping.cost");
    }

    private JsonNode firstPayment(JsonNode order) {
        JsonNode payments = order.path("payments");
        if (payments.isArray() && !payments.isEmpty()) {
            return payments.get(0);
        }
        return null;
    }

    private PaymentMethod paymentMethod(JsonNode payment) {
        if (payment == null || payment.isMissingNode() || payment.isNull()) {
            return PaymentMethod.UNKNOWN;
        }
        String value = textOrDefault(text(payment, "payment_type"), text(payment, "payment_method_id")).toLowerCase(Locale.ROOT);
        if (value.contains("pix")) {
            return PaymentMethod.PIX;
        }
        if (value.contains("credit") || value.contains("debit") || value.contains("card")) {
            return PaymentMethod.CARD;
        }
        if (value.contains("ticket") || value.contains("boleto")) {
            return PaymentMethod.BOLETO;
        }
        if (value.contains("account") || value.contains("balance")) {
            return PaymentMethod.MARKETPLACE_BALANCE;
        }
        return PaymentMethod.UNKNOWN;
    }

    private OrderStatus orderStatus(String status) {
        String normalizedStatus = status == null ? "" : status.toLowerCase(Locale.ROOT);
        if (normalizedStatus.contains("paid") || normalizedStatus.contains("confirmed")) {
            return OrderStatus.PAID;
        }
        if (normalizedStatus.contains("cancel")) {
            return OrderStatus.CANCELLED;
        }
        return OrderStatus.PENDING;
    }

    private String statusValue(OrderStatus status) {
        return switch (status) {
            case PAID -> "paid";
            case PENDING -> "pending";
            case CANCELLED -> "cancelled";
        };
    }

    private String buyerName(JsonNode order) {
        JsonNode buyer = order.path("buyer");
        String name = textOrDefault(text(buyer, "nickname"), text(buyer, "first_name"));
        String lastName = text(buyer, "last_name");
        if (!name.isBlank() && !lastName.isBlank()) {
            return name + " " + lastName;
        }
        return textOrDefault(name, "Comprador Mercado Livre");
    }

    private LocalDate paymentDate(JsonNode order) {
        JsonNode payment = firstPayment(order);
        LocalDate date = date(payment, "date_approved", "date_created");
        return date == null ? date(order, "date_created", "date_closed") : date;
    }

    private LocalDate releaseDate(JsonNode order) {
        JsonNode payment = firstPayment(order);
        LocalDate date = date(payment, "money_release_date", "available_date");
        return date == null ? paymentDate(order) : date;
    }

    private LocalDate date(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            String value = text(node, field);
            if (!value.isBlank()) {
                return parseDate(value);
            }
        }
        return null;
    }

    private LocalDate parseDate(String value) {
        try {
            return Instant.parse(value).atZone(SAO_PAULO).toLocalDate();
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant().atZone(SAO_PAULO).toLocalDate();
            } catch (DateTimeParseException ignoredAgain) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(SAO_PAULO).toLocalDate();
            }
        }
    }

    private String startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(SAO_PAULO).toInstant().toString();
    }

    private String endOfDayUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay(SAO_PAULO).minusNanos(1).toInstant().toString();
    }

    private BigDecimal firstAmount(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        for (String field : fields) {
            JsonNode value = nested(node, field);
            if (value != null && !value.isMissingNode() && !value.isNull()) {
                if (value.isNumber()) {
                    return value.decimalValue();
                }
                String text = value.asText("");
                if (!text.isBlank()) {
                    return new BigDecimal(text);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private JsonNode nested(JsonNode node, String field) {
        JsonNode current = node;
        for (String part : field.split("\\.")) {
            current = current.path(part);
        }
        return current;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = nested(node, field);
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ConnectorValidationException(message);
        }
        return value.trim();
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? (fallback == null ? "" : fallback) : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean configured() {
        return !configValue(clientId).isBlank() && !configValue(clientSecret).isBlank();
    }

    private void requireOAuthConfig() {
        if (!configured()) {
            throw new ConnectorValidationException("mercado_livre_oauth_not_configured");
        }
    }

    private String configValue(Optional<String> value) {
        return value == null ? "" : value.orElse("").trim();
    }
}
