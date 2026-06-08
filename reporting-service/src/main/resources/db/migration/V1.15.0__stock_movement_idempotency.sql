CREATE UNIQUE INDEX IF NOT EXISTS idx_stock_movements_reference
    ON stock_movements (tenant_id, stock_item_id, reference_type, reference_id, movement_type);
