// Models mapeados diretamente do core-service (Quarkus).
// O core-service retorna DTOs em snake_case; os nomes abaixo acompanham o contrato HTTP.

export type ConnectorConnectionStatus = 'active' | 'expired' | 'disconnected' | 'unavailable';

export interface ConnectorDescriptor {
  name: string;
  display_name: string;
  supports_invoices: boolean;
  required_methods: string[];
  optional_methods: string[];
}

export interface ConnectorStatus {
  platform: string;
  status: ConnectorConnectionStatus;
  message: string;
  checked_at: string;
}

export interface ConnectorToken {
  platform: string;
  status: ConnectorConnectionStatus;
  expires_at: string;
}

export interface AuthenticateRequest {
  credentials: Record<string, string>;
}

export interface SyncAllRequest {
  since?: string;
}

export interface SyncAccepted {
  job_id: string;
  status: string;
  connector_name: string;
  tenant_id: string;
  since: string | null;
  queued_at: string;
}

export interface SyncJob {
  job_id: string;
  tenant_id: string;
  connector_name: string;
  since: string | null;
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  recipient_email: string;
  requested_at: string;
  started_at: string | null;
  finished_at: string | null;
  error_message: string | null;
  orders_synced: number | null;
  payments_synced: number | null;
  fees_synced: number | null;
}

export interface StandardOrderItem {
  sku: string;
  title: string;
  quantity: number;
  unit_value: number;
  gross_value: number;
}

export interface StandardOrder {
  order_id: string;
  platform: string;
  date: string;
  gross_value: number;
  platform_fee: number;
  net_value: number;
  payment_method: string;
  payment_date: string;
  release_date: string;
  status: 'paid' | 'pending' | 'cancelled';
  buyer_name: string;
  items: StandardOrderItem[];
  invoice_number: string;
}

export interface PaymentInfo {
  payment_id: string;
  order_id: string;
  payment_method: string;
  gross_value: number;
  net_value: number;
  payment_date: string;
  release_date: string;
  status: string;
}

export interface FeeInfo {
  order_id: string;
  type: string;
  description: string;
  amount: number;
}

export interface InvoiceInfo {
  invoice_number: string;
  order_id: string;
  issued_at: string;
  status: string;
  access_key: string;
}

export interface TenantContext {
  tenantId: string;
  userId: string;
  email: string;
  roles: string[];
  readOnly: boolean;
}
