ALTER TABLE accounting_period_closings
    ADD COLUMN IF NOT EXISTS distributable_profit DECIMAL(15, 2);
