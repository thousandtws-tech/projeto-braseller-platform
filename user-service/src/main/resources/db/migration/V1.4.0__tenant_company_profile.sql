ALTER TABLE tenants ADD COLUMN cnpj VARCHAR(18);
ALTER TABLE tenants ADD COLUMN cnae_code VARCHAR(16);
ALTER TABLE tenants ADD COLUMN cnae_description VARCHAR(255);
ALTER TABLE tenants ADD COLUMN address_street VARCHAR(160);
ALTER TABLE tenants ADD COLUMN address_number VARCHAR(32);
ALTER TABLE tenants ADD COLUMN address_complement VARCHAR(160);
ALTER TABLE tenants ADD COLUMN address_neighborhood VARCHAR(120);
ALTER TABLE tenants ADD COLUMN address_city VARCHAR(120);
ALTER TABLE tenants ADD COLUMN address_state VARCHAR(2);
ALTER TABLE tenants ADD COLUMN address_zip_code VARCHAR(16);

CREATE INDEX idx_tenants_cnpj ON tenants(cnpj);
