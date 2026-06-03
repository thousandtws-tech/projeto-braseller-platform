// DTOs mapeados diretamente do reporting-service (Quarkus/OpenAPI).
// O contrato HTTP retorna snake_case; os nomes abaixo acompanham a API.

export type ReportEntryStatus = 'PAID' | 'PENDING_RELEASE' | 'RECEIVED' | 'CANCELLED' | 'REFUNDED';

export type PaymentMethod =
  | 'PIX'
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'BOLETO'
  | 'BANK_TRANSFER'
  | 'MARKETPLACE_BALANCE'
  | 'OTHER';

export type ReportExportFormat = 'PDF' | 'XLSX' | 'CSV';

export type TaxRegime = 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL';

export type ExpenseCategory =
  | 'ALUGUEL'
  | 'ENERGIA'
  | 'AGUA'
  | 'INTERNET'
  | 'TELEFONE'
  | 'MANUTENCAO'
  | 'MATERIAL'
  | 'SERVICOS'
  | 'TRANSPORTES'
  | 'OUTRAS';

export type DreCalculationStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface FinancialSummary {
  tenant_id: string;
  gross_value: number;
  received_value: number;
  fee_value: number;
  receivable_value: number;
  entry_count: number;
}

export interface ReportEntry {
  id: string;
  tenant_id: string;
  platform: string;
  order_id: string;
  sale_date: string;
  gross_value: number;
  received_value: number;
  fee_value: number;
  receivable_value: number;
  payment_method: PaymentMethod;
  status: ReportEntryStatus;
  release_date: string | null;
  buyer_name: string | null;
  invoice_number: string | null;
  created_at: string;
  updated_at: string;
}

export interface ReportEntryPage {
  items: ReportEntry[];
  total: number;
  page: number;
  size: number;
}

export interface AvailableFilters {
  platforms: string[];
  payment_methods: PaymentMethod[];
  statuses: ReportEntryStatus[];
}

export interface MonthlyEvolutionPoint {
  period: string;
  gross_value: number;
  received_value: number;
  fee_value: number;
  receivable_value: number;
  entry_count: number;
}

export interface PlatformComparisonPoint {
  platform: string;
  gross_value: number;
  received_value: number;
  fee_value: number;
  receivable_value: number;
  entry_count: number;
}

export interface DashboardView {
  summary: FinancialSummary;
  entries: ReportEntryPage;
  monthly_evolution: MonthlyEvolutionPoint[];
  platform_comparison: PlatformComparisonPoint[];
  filters: AvailableFilters;
}

export interface ReportFilters {
  from?: string;
  to?: string;
  platform?: string;
  paymentMethod?: string;
  status?: string;
  search?: string;
  sort?: string;
  direction?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}

export interface PublicReportEntryImportRequest {
  platform: string;
  order_id?: string;
  sale_date: string;
  gross_value: number;
  received_value: number;
  fee_value: number;
  receivable_value?: number;
  payment_method: string;
  status: string;
  release_date?: string;
  buyer_name?: string;
  invoice_number?: string;
}

export interface ReportEntryIngestRequest extends PublicReportEntryImportRequest {
  tenant_id: string;
}

export interface FiscalProfile {
  tenant_id: string;
  tax_regime: TaxRegime;
  estimated_tax_rate: number;
  notes: string | null;
  created_at: string;
  updated_at: string;
}

export interface FiscalProfileRequest {
  tax_regime: TaxRegime;
  estimated_tax_rate: number;
  notes?: string;
}

export interface CloudinaryAttachmentRequest {
  public_id: string;
  secure_url: string;
  resource_type: string;
  original_filename: string;
  content_type: string;
  size_bytes: number;
}

export type ExpenseAttachment = CloudinaryAttachmentRequest;

export interface ExpenseEntry {
  id: string;
  tenant_id: string;
  expense_date: string;
  category: ExpenseCategory;
  description: string;
  amount: number;
  attachment: ExpenseAttachment | null;
  created_at: string;
  updated_at: string;
}

export interface ExpensePage {
  items: ExpenseEntry[];
  total: number;
  page: number;
  size: number;
}

export interface ExpenseRequest {
  expense_date: string;
  category: string;
  description: string;
  amount: number;
  attachment?: CloudinaryAttachmentRequest;
}

export interface CloudinaryUploadSignature {
  cloud_name: string;
  api_key: string;
  upload_url: string;
  resource_type: string;
  folder: string;
  timestamp: number;
  use_filename: boolean;
  unique_filename: boolean;
  signature: string;
}

export interface ExpenseCategoryTotal {
  category: ExpenseCategory;
  amount: number;
  entry_count: number;
}

export interface DreStatement {
  tenant_id: string;
  from: string;
  to: string;
  tax_regime: TaxRegime;
  estimated_tax_rate: number;
  gross_revenue: number;
  marketplace_fees: number;
  estimated_taxes: number;
  operating_expenses: number;
  net_result: number;
  sales_count: number;
  expense_count: number;
  expenses_by_category: ExpenseCategoryTotal[];
}

export interface DreCalculationRequest {
  from: string;
  to: string;
}

export interface DreCalculationJob {
  job_id: string;
  tenant_id: string;
  from: string;
  to: string;
  status: DreCalculationStatus;
  requested_by_user_id: string;
  requested_by_email: string;
  requested_at: string;
  started_at: string | null;
  finished_at: string | null;
  error_message: string | null;
  statement: DreStatement | null;
}

export interface AccountingPeriodClosing {
  tenant_id: string;
  period_month: string;
  signed_by_user_id: string | null;
  signed_by_email: string | null;
  signature_hash: string | null;
  signed_at: string | null;
}

export interface AccountingPeriodSignatureRequest {
  signature_hash: string;
}

export interface PaymentReleaseAlert {
  tenant_id: string;
  platform: string;
  payment_id: string;
  amount: number;
  release_date: string;
}
