package com.example.application.port.out;

import com.example.domain.model.PurchaseEntry;
import com.example.domain.model.StockItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockRepository {
    StockItem upsertItem(String tenantId, String sku, String description, BigDecimal unitCost);
    Optional<StockItem> findItemBySku(String tenantId, String sku);
    List<StockItem> listItems(String tenantId);

    PurchaseEntry savePurchaseEntry(PurchaseEntry entry);
    List<PurchaseEntry> listPurchaseEntries(String tenantId, LocalDate from, LocalDate to);

    BigDecimal sumCmv(String tenantId, LocalDate from, LocalDate to);
    List<SaleStockMovement> listSaleExitMovements(String tenantId, String orderId);
    void recordMovement(String tenantId, String stockItemId, String movementType,
                        BigDecimal quantity, BigDecimal unitCost,
                        String referenceId, String referenceType, LocalDate date);

    record SaleStockMovement(String stockItemId, BigDecimal quantity, BigDecimal unitCost) {
    }
}
