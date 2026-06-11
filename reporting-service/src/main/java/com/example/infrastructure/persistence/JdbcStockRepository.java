package com.example.infrastructure.persistence;

import com.example.application.port.out.StockRepository;
import com.example.domain.model.PurchaseEntry;
import com.example.domain.model.PurchaseEntryItem;
import com.example.domain.model.StockItem;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcStockRepository implements StockRepository {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public StockItem upsertItem(String tenantId, String sku, String description, BigDecimal unitCost) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated = updateItem(conn, tenantId, sku, description, unitCost);
                if (updated == 0) {
                    try {
                        insertItem(conn, tenantId, sku, description, unitCost);
                    } catch (SQLException e) {
                        if (!isConstraintViolation(e) || updateItem(conn, tenantId, sku, description, unitCost) != 1) {
                            throw e;
                        }
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (RepositoryException e) {
            throw e;
        } catch (SQLException e) {
            throw new RepositoryException("stock_item_upsert_failed", e);
        } catch (Exception e) {
            throw new RepositoryException("stock_item_upsert_failed", e);
        }
        return findItemBySku(tenantId, sku).orElseThrow(() -> new RepositoryException("stock_item_not_found", null));
    }

    private int updateItem(Connection conn, String tenantId, String sku, String description, BigDecimal unitCost) throws SQLException {
        String sql = """
                UPDATE stock_items
                SET description = ?,
                    unit_cost = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE tenant_id = ? AND sku = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setBigDecimal(2, unitCost);
            ps.setString(3, tenantId);
            ps.setString(4, sku);
            return ps.executeUpdate();
        }
    }

    private void insertItem(Connection conn, String tenantId, String sku, String description, BigDecimal unitCost) throws SQLException {
        String sql = """
                INSERT INTO stock_items (id, tenant_id, sku, description, unit_cost, quantity, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, tenantId);
            ps.setString(3, sku);
            ps.setString(4, description);
            ps.setBigDecimal(5, unitCost);
            ps.executeUpdate();
        }
    }

    private boolean isConstraintViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }

    @Override
    public Optional<StockItem> findItemBySku(String tenantId, String sku) {
        String sql = "SELECT id, tenant_id, sku, description, unit_cost, quantity, created_at, updated_at FROM stock_items WHERE tenant_id = ? AND sku = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapItem(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("stock_item_find_failed", e);
        }
    }

    @Override
    public List<StockItem> listItems(String tenantId) {
        String sql = "SELECT id, tenant_id, sku, description, unit_cost, quantity, created_at, updated_at FROM stock_items WHERE tenant_id = ? ORDER BY sku";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockItem> items = new ArrayList<>();
                while (rs.next()) items.add(mapItem(rs));
                return items;
            }
        } catch (SQLException e) {
            throw new RepositoryException("stock_items_list_failed", e);
        }
    }

    @Override
    public PurchaseEntry savePurchaseEntry(PurchaseEntry entry) {
        String entryId = entry.id() != null ? entry.id() : UUID.randomUUID().toString();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO purchase_entries (id, tenant_id, nfe_number, supplier_name, issue_date, total_cost, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)")) {
                    ps.setString(1, entryId);
                    ps.setString(2, entry.tenantId());
                    ps.setString(3, entry.nfeNumber());
                    ps.setString(4, entry.supplierName());
                    ps.setDate(5, Date.valueOf(entry.issueDate()));
                    ps.setBigDecimal(6, entry.totalCost());
                    ps.executeUpdate();
                }
                List<PurchaseEntryItem> savedItems = new ArrayList<>();
                for (PurchaseEntryItem item : entry.items()) {
                    String itemId = UUID.randomUUID().toString();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO purchase_entry_items (id, purchase_entry_id, sku, description, quantity, unit_cost, total_cost) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setString(1, itemId);
                        ps.setString(2, entryId);
                        ps.setString(3, item.sku());
                        ps.setString(4, item.description());
                        ps.setBigDecimal(5, item.quantity());
                        ps.setBigDecimal(6, item.unitCost());
                        ps.setBigDecimal(7, item.totalCost());
                        ps.executeUpdate();
                    }
                    savedItems.add(new PurchaseEntryItem(itemId, entryId, item.sku(), item.description(), item.quantity(), item.unitCost(), item.totalCost()));
                }
                conn.commit();
                return new PurchaseEntry(entryId, entry.tenantId(), entry.nfeNumber(), entry.supplierName(), entry.issueDate(), entry.totalCost(), savedItems, null);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("purchase_entry_save_failed", e instanceof SQLException ? (SQLException) e : null);
        }
    }

    @Override
    public List<PurchaseEntry> listPurchaseEntries(String tenantId, LocalDate from, LocalDate to) {
        String sql = "SELECT id, tenant_id, nfe_number, supplier_name, issue_date, total_cost, created_at FROM purchase_entries WHERE tenant_id = ? AND issue_date BETWEEN ? AND ? ORDER BY issue_date DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<PurchaseEntry> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new PurchaseEntry(rs.getString("id"), rs.getString("tenant_id"),
                            rs.getString("nfe_number"), rs.getString("supplier_name"),
                            rs.getDate("issue_date").toLocalDate(), rs.getBigDecimal("total_cost"),
                            List.of(), null));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("purchase_entries_list_failed", e);
        }
    }

    @Override
    public BigDecimal sumCmv(String tenantId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN movement_type = 'EXIT' THEN quantity * unit_cost
                        WHEN movement_type = 'SALE_REVERSAL' THEN -quantity * unit_cost
                        ELSE 0
                    END
                ), 0)
                FROM stock_movements
                WHERE tenant_id = ?
                  AND reference_type = 'SALE'
                  AND movement_type IN ('EXIT', 'SALE_REVERSAL')
                  AND movement_date BETWEEN ? AND ?
                """;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RepositoryException("cmv_sum_failed", e);
        }
    }

    @Override
    public BigDecimal totalInventoryValue(String tenantId) {
        String sql = "SELECT COALESCE(SUM(quantity * unit_cost), 0) FROM stock_items WHERE tenant_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RepositoryException("stock_inventory_value_failed", e);
        }
    }

    @Override
    public List<StockRepository.SaleStockMovement> listSaleExitMovements(String tenantId, String orderId) {
        String sql = """
                SELECT stock_item_id, quantity, unit_cost
                FROM stock_movements
                WHERE tenant_id = ?
                  AND reference_type = 'SALE'
                  AND reference_id = ?
                  AND movement_type = 'EXIT'
                ORDER BY created_at ASC
                """;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockRepository.SaleStockMovement> movements = new ArrayList<>();
                while (rs.next()) {
                    movements.add(new StockRepository.SaleStockMovement(
                            rs.getString("stock_item_id"),
                            rs.getBigDecimal("quantity"),
                            rs.getBigDecimal("unit_cost")
                    ));
                }
                return movements;
            }
        } catch (SQLException e) {
            throw new RepositoryException("sale_stock_movements_list_failed", e);
        }
    }

    @Override
    public void recordMovement(String tenantId, String stockItemId, String movementType,
                               BigDecimal quantity, BigDecimal unitCost,
                               String referenceId, String referenceType, LocalDate date) {
        BigDecimal delta = movementDelta(movementType, quantity);
        String sql = "INSERT INTO stock_movements (id, tenant_id, stock_item_id, movement_type, quantity, unit_cost, reference_id, reference_type, movement_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        String updateStockSql = "UPDATE stock_items SET quantity = quantity + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, tenantId);
                    ps.setString(3, stockItemId);
                    ps.setString(4, movementType);
                    ps.setBigDecimal(5, quantity);
                    ps.setBigDecimal(6, unitCost);
                    ps.setString(7, referenceId);
                    ps.setString(8, referenceType);
                    ps.setDate(9, Date.valueOf(date));
                    ps.executeUpdate();
                } catch (SQLException e) {
                    if (isUniqueConstraintViolation(e)) {
                        conn.rollback();
                        return;
                    }
                    throw e;
                }
                try (PreparedStatement ps = conn.prepareStatement(updateStockSql)) {
                    ps.setBigDecimal(1, delta);
                    ps.setString(2, stockItemId);
                    ps.setString(3, tenantId);
                    if (ps.executeUpdate() != 1) {
                        throw new RepositoryException("stock_item_quantity_update_failed", null);
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (RepositoryException e) {
            throw e;
        } catch (SQLException e) {
            throw new RepositoryException("stock_movement_record_failed", e);
        } catch (Exception e) {
            throw new RepositoryException("stock_movement_record_failed", e);
        }
    }

    private BigDecimal movementDelta(String movementType, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new RepositoryException("stock_movement_quantity_invalid", null);
        }
        if ("ENTRY".equalsIgnoreCase(movementType)) {
            return quantity;
        }
        if ("SALE_REVERSAL".equalsIgnoreCase(movementType)) {
            return quantity;
        }
        if ("EXIT".equalsIgnoreCase(movementType)) {
            return quantity.negate();
        }
        throw new RepositoryException("stock_movement_type_invalid", null);
    }

    private boolean isUniqueConstraintViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }

    private StockItem mapItem(ResultSet rs) throws SQLException {
        return new StockItem(rs.getString("id"), rs.getString("tenant_id"),
                rs.getString("sku"), rs.getString("description"),
                rs.getBigDecimal("unit_cost"), rs.getBigDecimal("quantity"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }
}
