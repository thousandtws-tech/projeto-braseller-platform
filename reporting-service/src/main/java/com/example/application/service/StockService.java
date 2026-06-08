package com.example.application.service;

import com.example.application.exception.ValidationException;
import com.example.application.port.out.StockRepository;
import com.example.domain.model.PurchaseEntry;
import com.example.domain.model.PurchaseEntryItem;
import com.example.domain.model.StockItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class StockService {

    @Inject
    StockRepository stockRepository;

    @Inject
    NfeXmlParserService nfeXmlParserService;

    public PurchaseEntry importNfeXml(String tenantId, String xmlContent) {
        PurchaseEntry entry = nfeXmlParserService.parse(tenantId, xmlContent);
        // Persist entry and update stock quantities
        PurchaseEntry saved = stockRepository.savePurchaseEntry(entry);
        for (PurchaseEntryItem item : saved.items()) {
            if (item.sku() != null && !item.sku().isBlank()) {
                StockItem stockItem = stockRepository.upsertItem(
                        tenantId, item.sku(), item.description(), item.unitCost());
                stockRepository.recordMovement(
                        tenantId, stockItem.id(), "ENTRY",
                        item.quantity(), item.unitCost(),
                        saved.id(), "PURCHASE", saved.issueDate());
            }
        }
        return saved;
    }

    public StockItem upsertStockItem(String tenantId, String sku, String description, BigDecimal unitCost) {
        if (sku == null || sku.isBlank()) throw new ValidationException("sku_required");
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("unit_cost_invalid");
        return stockRepository.upsertItem(tenantId, sku.trim(), description == null ? sku : description.trim(), unitCost);
    }

    public List<StockItem> listItems(String tenantId) {
        return stockRepository.listItems(tenantId);
    }

    public List<PurchaseEntry> listPurchaseEntries(String tenantId, LocalDate from, LocalDate to) {
        return stockRepository.listPurchaseEntries(tenantId, from, to);
    }

    public BigDecimal calculateCmv(String tenantId, LocalDate from, LocalDate to) {
        return stockRepository.sumCmv(tenantId, from, to);
    }

    public void recordSaleMovement(String tenantId, String sku, BigDecimal quantity, String orderId, LocalDate saleDate) {
        if (sku == null || sku.isBlank()
                || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
                || orderId == null || orderId.isBlank()
                || saleDate == null) {
            return;
        }
        stockRepository.findItemBySku(tenantId, sku.trim()).ifPresent(item ->
                stockRepository.recordMovement(tenantId, item.id(), "EXIT",
                        quantity, item.unitCost(), orderId.trim(), "SALE", saleDate));
    }

    public void reverseSaleMovements(String tenantId, String orderId, LocalDate saleDate) {
        if (tenantId == null || tenantId.isBlank()
                || orderId == null || orderId.isBlank()
                || saleDate == null) {
            return;
        }
        for (StockRepository.SaleStockMovement movement : stockRepository.listSaleExitMovements(tenantId, orderId.trim())) {
            stockRepository.recordMovement(
                    tenantId,
                    movement.stockItemId(),
                    "SALE_REVERSAL",
                    movement.quantity(),
                    movement.unitCost(),
                    orderId.trim(),
                    "SALE",
                    saleDate
            );
        }
    }
}
