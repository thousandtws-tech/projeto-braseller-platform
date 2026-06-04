// ─── Auth ────────────────────────────────────────────────────────────────────
export interface AuthTokenSet {
  accessToken: string
  refreshToken: string
  expiresAt: string
  tenantId: string
  userId: string
  email: string
  roles: string[]
}

export interface UserSession {
  tenantId: string
  userId: string
  email: string
  fullName: string
  roles: string[]
}

export interface UserProfile {
  id: string
  tenantId: string
  email: string
  fullName: string
  preferredUsername: string
  firstName: string
  lastName: string
  pictureUrl?: string
  emailVerified: boolean
  provider: string
  providerSubject: string
  status: string
  roles: string[]
}

// ─── Dashboard (legacy camelCase — mantido para componentes existentes) ───────
export interface FinancialSummary {
  grossRevenue: number
  received: number
  fees: number
  receivable: number
  totalOrders: number
}

export interface MonthlyEvolution {
  month: string
  grossRevenue: number
  received: number
  fees: number
  netProfit: number
}

export interface PlatformBreakdown {
  platform: string
  percentage: number
  amount: number
}

export interface ReportEntry {
  id: string
  platform: string
  orderId: string
  saleDate: string
  grossValue: number
  receivedValue: number
  feeValue: number
  receivableValue: number
  paymentMethod: string
  status: 'PAID' | 'PENDING' | 'CANCELLED' | 'REFUNDED'
  releaseDate: string
  buyerName: string
  invoiceNumber?: string
}

export interface ExpenseEntry {
  id: string
  expenseDate: string
  category: string
  description: string
  amount: number
  attachmentUrl?: string
}

export interface DreStatement {
  grossRevenue: number
  fees: number
  taxes: number
  expenses: number
  netProfit: number
  profitMargin: number
  period: string
}

export interface DashboardView extends FinancialSummary {
  monthlyEvolution: MonthlyEvolution[]
  platformBreakdown: PlatformBreakdown[]
  recentOrders: ReportEntry[]
}

// ─── Reports-service (snake_case, resposta real da API) ───────────────────────
export interface ReportsSummary {
  tenant_id: string
  gross_value: number
  received_value: number
  fee_value: number
  receivable_value: number
  entry_count: number
}

export interface ReportsEntry {
  id: string
  tenant_id: string
  platform: string
  order_id: string
  sale_date: string
  gross_value: number
  received_value: number
  fee_value: number
  receivable_value: number
  payment_method: string
  status: 'PAID' | 'PENDING' | 'CANCELLED' | 'REFUNDED'
  release_date: string
  buyer_name: string
  invoice_number?: string
  created_at?: string
  updated_at?: string
}

export interface Paginated<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface CloudinaryAttachment {
  public_id: string
  secure_url: string
  resource_type: string
  original_filename: string
  content_type: string
  size_bytes: number
}

export interface ReportsExpense {
  id: string
  tenant_id: string
  expense_date: string
  category: string
  description: string
  amount: number
  attachment?: CloudinaryAttachment
  created_at?: string
  updated_at?: string
}

export interface MonthlyEvolutionItem {
  period: string
  gross_value: number
  received_value: number
  fee_value: number
  receivable_value: number
  entry_count: number
}

export interface PlatformComparisonItem {
  platform: string
  gross_value: number
  received_value: number
  fee_value: number
  receivable_value: number
  entry_count: number
}

export interface ReportsDashboard {
  summary: ReportsSummary
  entries: Paginated<ReportsEntry>
  monthly_evolution: MonthlyEvolutionItem[]
  platform_comparison: PlatformComparisonItem[]
  filters: ReportsFilters
}

export interface ReportsFilters {
  platforms: string[]
  payment_methods: string[]
  statuses: string[]
}

export interface ReportsDre {
  tenant_id: string
  from: string
  to: string
  tax_regime: string
  estimated_tax_rate: number
  gross_revenue: number
  marketplace_fees: number
  estimated_taxes: number
  operating_expenses: number
  net_result: number
  sales_count: number
  expense_count: number
  expenses_by_category: { category: string; amount: number; entry_count: number }[]
}

export interface DreJob {
  job_id: string
  tenant_id: string
  from: string
  to: string
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETE' | 'ERROR'
  requested_by_email?: string
  requested_at?: string
  started_at?: string
  finished_at?: string
  error_message?: string
  statement?: ReportsDre
}

export interface FiscalProfile {
  tenant_id: string
  tax_regime: 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL' | 'MEI'
  estimated_tax_rate: number
  notes?: string
  created_at?: string
  updated_at?: string
}

export interface CloudinaryUploadSignature {
  cloud_name: string
  api_key: string
  upload_url: string
  resource_type: string
  folder: string
  timestamp: number
  use_filename: boolean
  unique_filename: boolean
  signature: string
}

export interface AccountingClosing {
  tenant_id: string
  period_month: string
  signed_by_user_id: string
  signed_by_email: string
  signature_hash: string
  signed_at: string
}

// ─── Core-service ─────────────────────────────────────────────────────────────
export interface CoreConnector {
  platform: string
  status: 'active' | 'inactive' | 'error' | 'syncing'
  message?: string
  checked_at?: string
}

export interface ConnectorStatus {
  name: string
  displayName: string
  status: 'connected' | 'disconnected' | 'error' | 'syncing'
  lastSync?: string
  totalOrders?: number
}

export interface SyncJob {
  job_id: string
  tenant_id: string
  connector_name: string
  since?: string
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETE' | 'ERROR'
  recipient_email?: string
  requested_at?: string
  started_at?: string
  finished_at?: string
  error_message?: string
  orders_synced: number
  payments_synced: number
  fees_synced: number
}

export interface CoreOrderItem {
  sku: string
  title: string
  quantity: number
  unit_value: number
  gross_value: number
}

export interface CoreOrder {
  order_id: string
  platform: string
  date: string
  gross_value: number
  platform_fee: number
  net_value: number
  payment_method: string
  payment_date?: string
  release_date?: string
  status: 'paid' | 'pending' | 'cancelled' | 'refunded'
  buyer_name: string
  items: CoreOrderItem[]
  invoice_number?: string
}

// ─── Notifications ────────────────────────────────────────────────────────────
export interface NotificationMessage {
  id: string
  tenantId?: string
  type: 'NEW_SALE' | 'ML_PAYMENT_RELEASE' | 'MONTHLY_CLOSING_SUMMARY' | 'WEEKLY_ACCOUNTANT_REPORT'
  title: string
  message: string
  recipientEmail?: string
  channel: 'IN_APP' | 'EMAIL'
  status: 'UNREAD' | 'READ'
  readAt?: string
  createdAt: string
}

export interface NotificationPreferences {
  tenantId: string
  emailEnabled: boolean
  newSaleEnabled: boolean
  monthlyClosingEnabled: boolean
  mlPaymentReleaseEnabled: boolean
  weeklyAccountantReportEnabled: boolean
  recipientEmail?: string
  accountantEmail?: string
  updatedAt?: string
}

// ─── Billing ──────────────────────────────────────────────────────────────────
export interface BillingSubscription {
  planCode: string
  planName: string
  status: 'TRIALING' | 'ACTIVE' | 'SUSPENDED' | 'CANCELLED'
  trialEndsAt?: string
  currentPeriodEnd?: string
}

// ─── Utilities ────────────────────────────────────────────────────────────────
export interface ApiError {
  message: string
  status: number
}

export type ActionState<T = void> =
  | { success: true; data: T }
  | { success: false; error: string }
  | null
