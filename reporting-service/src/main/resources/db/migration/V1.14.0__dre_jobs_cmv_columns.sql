ALTER TABLE dre_calculation_jobs
    ADD COLUMN IF NOT EXISTS cmv              DECIMAL(15, 2),
    ADD COLUMN IF NOT EXISTS banking_expenses DECIMAL(15, 2),
    ADD COLUMN IF NOT EXISTS distributable_profit DECIMAL(15, 2);
