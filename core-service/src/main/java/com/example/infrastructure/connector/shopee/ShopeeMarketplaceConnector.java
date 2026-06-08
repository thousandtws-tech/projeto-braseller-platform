package com.example.infrastructure.connector.shopee;

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
import com.example.domain.model.connector.StandardOrderItem;
import com.example.domain.model.connector.SyncResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ShopeeMarketplaceConnector implements MarketplaceConnector {
    private static final String NAME = "shopee";
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final int DEFAULT_TOKEN_TTL_SECONDS = 14400;
    private static final int MAX_ORDER_DETAIL_BATCH = 50;

    @Inject
    ShopeeApiClient apiClient;

    @Inject
    JdbcShopeeTokenRepository tokenRepository;

    @ConfigProperty(name = "shopee.oauth.partner-id", defaultValue = "0")
    long partnerId;

    @ConfigProperty(name = "shopee.oauth.partner-key", defaultValue = "")
    Optional<String> partnerKey;

    @ConfigProperty(name = "shopee.oauth.refresh-skew-seconds", defaultValue = "300")
    long refreshSkewSeconds;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ConnectorDescriptor descriptor() {
        return new ConnectorDescriptor(
                NAME,
                "Shopee",
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
        String code = requireText(credentials.get("code"), "shopee_oauth_code_required");
        String shopIdStr = requireText(credentials.get("shop_id"), "shopee_shop_id_required");
        long shopId = parseLong(shopIdStr, "shopee_shop_id_invalid");

        ObjectNode body = apiClient.newBody();
        body.put("partner_id", partnerId);
        body.put("code", code);
        body.put("shop_id", shopId);
        JsonNode response = apiClient.postPublic("/api/v2/auth/token/get", partnerId, partnerKeyValue(), body);

        ShopeeConnectorToken token = tokenFromResponse(command.tenantId(), String.valueOf(shopId), response);
        tokenRepository.save(token);
        return toConnectorToken(token);
    }

    @Override
    public ConnectorToken refreshToken(ConnectorRefreshTokenCommand command) {
        requireOAuthConfig();
        ShopeeConnectorToken current = tokenRepository.find(command.tenantId())
                .orElseThrow(() -> new ConnectorValidationException("shopee_not_authenticated"));
        ShopeeConnectorToken token = refresh(command.tenantId(), current.shopId(), current.refreshToken());
        return toConnectorToken(token);
    }

    @Override
    public List<StandardOrder> getOrders(String tenantId, OrderFilters filters) {
        ShopeeConnectorToken token = validToken(tenantId);
        OrderFilters applied = filters == null ? new OrderFilters(null, null, null, 50) : filters;
        LocalDate from = applied.from() != null ? applied.from() : LocalDate.now(SAO_PAULO).minusDays(1);
        LocalDate to = applied.to() != null ? applied.to() : LocalDate.now(SAO_PAULO);

        String path = "/api/v2/order/get_order_list";
        long shopId = parseLong(token.shopId(), "shopee_shop_id_invalid");

        StringBuilder query = new StringBuilder();
        query.append("time_range_field=create_time");
        query.append("&time_from=").append(from.atStartOfDay(SAO_PAULO).toEpochSecond());
        query.append("&time_to=").append(to.plusDays(1).atStartOfDay(SAO_PAULO).toEpochSecond());
        query.append("&page_size=").append(Math.min(applied.limit(), 100));
        if (applied.status() != null) {
            query.append("&order_status=").append(statusValue(applied.status()));
        }

        JsonNode response = apiClient.get(path + "?" + query, partnerId, partnerKeyValue(), shopId, token.accessToken());
        JsonNode orderList = response.path("response").path("order_list");
        if (orderList.isMissingNode() || !orderList.isArray() || orderList.isEmpty()) {
            return List.of();
        }

        List<String> orderIds = new ArrayList<>();
        for (JsonNode entry : orderList) {
            String sn = text(entry, "order_sn");
            if (!sn.isBlank()) orderIds.add(sn);
        }
        return fetchOrderDetails(token, orderIds);
    }

    @Override
    public StandardOrder getOrderDetail(String tenantId, String orderId) {
        ShopeeConnectorToken token = validToken(tenantId);
        List<StandardOrder> orders = fetchOrderDetails(token, List.of(requireText(orderId, "order_id_required")));
        if (orders.isEmpty()) {
            throw new ConnectorValidationException("shopee_order_not_found");
        }
        return orders.get(0);
    }

    @Override
    public List<PaymentInfo> getPayments(String tenantId, String orderId) {
        ShopeeConnectorToken token = validToken(tenantId);
        JsonNode escrow = escrowDetail(token, requireText(orderId, "order_id_required"));
        JsonNode resp = escrow.path("response");
        BigDecimal escrowAmount = decimal(resp, "escrow_amount");
        BigDecimal originalPrice = itemsTotal(resp);
        return List.of(new PaymentInfo(
                "PAY-" + orderId,
                orderId,
                PaymentMethod.UNKNOWN,
                money(originalPrice),
                money(escrowAmount),
                null,
                null,
                "released"
        ));
    }

    @Override
    public List<FeeInfo> getFees(String tenantId, String orderId) {
        ShopeeConnectorToken token = validToken(tenantId);
        JsonNode escrow = escrowDetail(token, requireText(orderId, "order_id_required"));
        JsonNode resp = escrow.path("response");
        List<FeeInfo> fees = new ArrayList<>();
        BigDecimal commission = feeFromResponse(resp, "commission_fee");
        BigDecimal serviceFee = feeFromResponse(resp, "service_fee");
        BigDecimal transactionFee = feeFromResponse(resp, "seller_transaction_fee", "transaction_fee");
        BigDecimal shippingCost = shippingCost(resp);
        if (isPositive(commission)) {
            fees.add(new FeeInfo(orderId, "commission_fee", "Comissão Shopee", money(commission)));
        }
        if (isPositive(serviceFee)) {
            fees.add(new FeeInfo(orderId, "service_fee", "Taxa de serviço Shopee", money(serviceFee)));
        }
        if (isPositive(transactionFee)) {
            fees.add(new FeeInfo(orderId, "transaction_fee", "Taxa de transacao Shopee", money(transactionFee)));
        }
        if (isPositive(shippingCost)) {
            fees.add(new FeeInfo(orderId, "shipping_cost", "Custo de envio Shopee", money(shippingCost)));
        }
        return fees;
    }

    @Override
    public List<InvoiceInfo> getInvoices(String tenantId, InvoiceFilters filters) {
        LocalDate from = filters != null && filters.from() != null ? filters.from() : LocalDate.now(SAO_PAULO).minusDays(30);
        LocalDate to = filters != null && filters.to() != null ? filters.to() : LocalDate.now(SAO_PAULO);
        int limit = filters != null ? filters.limit() : 50;

        OrderFilters orderFilters = new OrderFilters(from, to, OrderStatus.PAID, limit);
        List<StandardOrder> orders = getOrders(tenantId, orderFilters);
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
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.DISCONNECTED, "shopee_oauth_not_configured", Instant.now());
        }
        return tokenRepository.find(tenantId)
                .map(this::statusForToken)
                .orElseGet(() -> new ConnectorStatus(NAME, ConnectorConnectionStatus.DISCONNECTED, "shopee_not_authenticated", Instant.now()));
    }

    private ConnectorStatus statusForToken(ShopeeConnectorToken token) {
        if (token.expiresAt().isBefore(Instant.now())) {
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.EXPIRED, "shopee_token_expired", Instant.now());
        }
        try {
            long shopId = parseLong(token.shopId(), "shopee_shop_id_invalid");
            apiClient.get("/api/v2/shop/get_shop_info", partnerId, partnerKeyValue(), shopId, token.accessToken());
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.ACTIVE, "shopee_connector_active", Instant.now());
        } catch (ConnectorValidationException e) {
            return new ConnectorStatus(NAME, ConnectorConnectionStatus.UNAVAILABLE, e.getMessage(), Instant.now());
        }
    }

    private List<StandardOrder> fetchOrderDetails(ShopeeConnectorToken token, List<String> orderIds) {
        List<StandardOrder> result = new ArrayList<>();
        long shopId = parseLong(token.shopId(), "shopee_shop_id_invalid");
        for (int i = 0; i < orderIds.size(); i += MAX_ORDER_DETAIL_BATCH) {
            List<String> batch = orderIds.subList(i, Math.min(i + MAX_ORDER_DETAIL_BATCH, orderIds.size()));
            String sns = String.join(",", batch);
            String path = "/api/v2/order/get_order_detail?order_sn_list=" + sns
                    + "&response_optional_fields=buyer_user_id,buyer_username,item_list,payment_method,total_amount";
            JsonNode response = apiClient.get(path, partnerId, partnerKeyValue(), shopId, token.accessToken());
            for (JsonNode order : response.path("response").path("order_list")) {
                result.add(toStandardOrder(order));
            }
        }
        return result;
    }

    private JsonNode escrowDetail(ShopeeConnectorToken token, String orderId) {
        long shopId = parseLong(token.shopId(), "shopee_shop_id_invalid");
        String path = "/api/v2/payment/get_escrow_detail?order_sn=" + orderId;
        return apiClient.get(path, partnerId, partnerKeyValue(), shopId, token.accessToken());
    }

    private StandardOrder toStandardOrder(JsonNode order) {
        String orderId = text(order, "order_sn");
        long createTime = order.path("create_time").asLong(0);
        LocalDate date = createTime > 0
                ? Instant.ofEpochSecond(createTime).atZone(SAO_PAULO).toLocalDate()
                : LocalDate.now(SAO_PAULO);
        BigDecimal grossValue = decimal(order, "total_amount");
        if (!isPositive(grossValue)) {
            grossValue = itemsTotal(order);
        }
        // Shopee escrow data is not inline in order detail — use estimated fee of ~12% if not available
        BigDecimal platformFee = decimal(order, "estimated_shipping_fee");
        return new StandardOrder(
                orderId,
                NAME,
                date,
                money(grossValue),
                money(platformFee),
                money(grossValue.subtract(platformFee)),
                paymentMethod(text(order, "payment_method")),
                date,
                date.plusDays(15),
                orderStatus(text(order, "order_status")),
                buyerName(order),
                items(order),
                text(order, "invoice_number")
        );
    }

    private BigDecimal itemsTotal(JsonNode node) {
        BigDecimal total = BigDecimal.ZERO;
        JsonNode items = node.path("item_list");
        if (items.isMissingNode()) items = node.path("items");
        for (JsonNode item : items) {
            BigDecimal price = decimal(item, "model_discounted_price");
            if (!isPositive(price)) price = decimal(item, "item_price");
            if (!isPositive(price)) price = decimal(item, "original_price");
            if (!isPositive(price)) price = decimal(item, "selling_price");
            int qty = item.path("model_quantity_purchased").asInt(0);
            if (qty == 0) qty = item.path("quantity_purchased").asInt(1);
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
        }
        return total;
    }

    private List<StandardOrderItem> items(JsonNode order) {
        List<StandardOrderItem> result = new ArrayList<>();
        for (JsonNode item : order.path("item_list")) {
            String sku = text(item, "item_sku");
            String title = text(item, "item_name");
            int qty = item.path("model_quantity_purchased").asInt(1);
            BigDecimal unitPrice = decimal(item, "model_discounted_price");
            if (!isPositive(unitPrice)) unitPrice = decimal(item, "item_price");
            result.add(new StandardOrderItem(sku, title, qty, money(unitPrice), money(unitPrice.multiply(BigDecimal.valueOf(qty)))));
        }
        return result;
    }

    private PaymentMethod paymentMethod(String method) {
        if (method == null) return PaymentMethod.UNKNOWN;
        String lower = method.toLowerCase(Locale.ROOT);
        if (lower.contains("pix")) return PaymentMethod.PIX;
        if (lower.contains("credit") || lower.contains("card") || lower.contains("debit")) return PaymentMethod.CARD;
        if (lower.contains("boleto")) return PaymentMethod.BOLETO;
        if (lower.contains("balance") || lower.contains("spay") || lower.contains("shopee")) return PaymentMethod.MARKETPLACE_BALANCE;
        return PaymentMethod.UNKNOWN;
    }

    private OrderStatus orderStatus(String status) {
        if (status == null) return OrderStatus.PENDING;
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "COMPLETED", "TO_CONFIRM_RECEIVE" -> OrderStatus.PAID;
            case "CANCELLED", "IN_CANCEL" -> OrderStatus.CANCELLED;
            default -> OrderStatus.PENDING;
        };
    }

    private String statusValue(OrderStatus status) {
        return switch (status) {
            case PAID -> "COMPLETED";
            case CANCELLED -> "CANCELLED";
            case PENDING -> "READY_TO_SHIP";
        };
    }

    private String buyerName(JsonNode order) {
        String name = text(order, "buyer_username");
        return name.isBlank() ? "Comprador Shopee" : name;
    }

    private ShopeeConnectorToken tokenFromResponse(String tenantId, String shopId, JsonNode response) {
        String accessToken = requireText(text(response, "access_token"), "shopee_access_token_missing");
        String refreshToken = requireText(text(response, "refresh_token"), "shopee_refresh_token_missing");
        long expireIn = response.path("expire_in").asLong(DEFAULT_TOKEN_TTL_SECONDS);
        String resolvedShopId = shopId;
        if (resolvedShopId == null || resolvedShopId.isBlank()) {
            JsonNode shopIds = response.path("shop_id_list");
            if (shopIds.isArray() && !shopIds.isEmpty()) {
                resolvedShopId = shopIds.get(0).asText("");
            }
        }
        return new ShopeeConnectorToken(tenantId, resolvedShopId, accessToken, refreshToken, Instant.now().plusSeconds(expireIn));
    }

    private ShopeeConnectorToken refresh(String tenantId, String shopId, String refreshToken) {
        long shopIdLong = parseLong(shopId, "shopee_shop_id_invalid");
        ObjectNode body = apiClient.newBody();
        body.put("partner_id", partnerId);
        body.put("refresh_token", refreshToken);
        body.put("shop_id", shopIdLong);
        JsonNode response = apiClient.postPublic("/api/v2/auth/access_token/get", partnerId, partnerKeyValue(), body);
        ShopeeConnectorToken token = tokenFromResponse(tenantId, shopId, response);
        tokenRepository.save(token);
        return token;
    }

    private ShopeeConnectorToken validToken(String tenantId) {
        requireOAuthConfig();
        ShopeeConnectorToken token = tokenRepository.find(tenantId)
                .orElseThrow(() -> new ConnectorValidationException("shopee_not_authenticated"));
        if (token.expiresAt().minusSeconds(refreshSkewSeconds).isBefore(Instant.now())) {
            return refresh(tenantId, token.shopId(), token.refreshToken());
        }
        return token;
    }

    private ConnectorToken toConnectorToken(ShopeeConnectorToken token) {
        return new ConnectorToken(NAME, ConnectorConnectionStatus.ACTIVE, token.expiresAt());
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        if (value == null || value.isMissingNode() || value.isNull()) return BigDecimal.ZERO;
        if (value.isNumber()) return value.decimalValue();
        String text = value.asText("");
        if (text.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(text); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private BigDecimal feeFromResponse(JsonNode response, String... fields) {
        JsonNode orderIncome = response == null ? null : response.path("order_income");
        for (String field : fields) {
            BigDecimal value = decimal(orderIncome, field);
            if (isPositive(value)) return value;
            value = decimal(response, field);
            if (isPositive(value)) return value;
        }
        BigDecimal total = BigDecimal.ZERO;
        JsonNode items = response == null ? null : response.path("items");
        if (items != null) {
            for (JsonNode item : items) {
                for (String field : fields) {
                    total = total.add(decimal(item, field));
                }
            }
        }
        return total;
    }

    private BigDecimal shippingCost(JsonNode response) {
        return firstPositive(response,
                "final_shipping_fee",
                "actual_shipping_fee",
                "estimated_shipping_fee",
                "seller_shipping_fee",
                "shipping_fee",
                "shipping_fee_seller_discount",
                "seller_shipping_discount");
    }

    private BigDecimal firstPositive(JsonNode response, String... fields) {
        JsonNode orderIncome = response == null ? null : response.path("order_income");
        for (String field : fields) {
            BigDecimal value = decimal(orderIncome, field);
            if (isPositive(value)) return value;
            value = decimal(response, field);
            if (isPositive(value)) return value;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new ConnectorValidationException(message);
        return value.trim();
    }

    private long parseLong(String value, String errorCode) {
        try { return Long.parseLong(value.trim()); }
        catch (NumberFormatException e) { throw new ConnectorValidationException(errorCode); }
    }

    private boolean configured() {
        return partnerId > 0 && !partnerKeyValue().isBlank();
    }

    private void requireOAuthConfig() {
        if (!configured()) throw new ConnectorValidationException("shopee_oauth_not_configured");
    }

    private String partnerKeyValue() {
        return partnerKey == null ? "" : partnerKey.orElse("").trim();
    }
}
