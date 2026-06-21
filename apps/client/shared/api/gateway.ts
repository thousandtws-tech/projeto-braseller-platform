import type {
  DashboardView,
  ReportEntry,
  ExpenseEntry,
  DreStatement,
  ConnectorStatus,
  CoreConnector,
  CoreOrder,
  IntegrationHealthSummary,
  IntegrationEventLog,
  SyncJob,
  NotificationMessage,
  NotificationPreferences,
  BillingSubscription,
  UserSession,
  UserProfile,
  AccountantClient,
  // Reports-service
  ReportsDashboard,
  ReportsSummary,
  ReportsEntry,
  ReportsExpense,
  ReportsDre,
  ReportsBalanceSheet,
  FiscalProfile,
  CloudinaryUploadSignature,
  Paginated,
  ReportsFilters,
  MonthlyEvolutionItem,
  PlatformComparisonItem,
  AccountingClosing,
  InvoiceEntry,
  StockItem,
  PurchaseEntry,
  BankTransaction,
  ProfitAvailability,
  ProfitDistribution,
} from '@/shared/types'

function resolveGatewayUrl() {
  const raw =
    process.env.GATEWAY_URL ??
    process.env.NEXT_PUBLIC_GATEWAY_URL ??
    'http://localhost:8080'

  return raw.trim().replace(/^["']|["']$/g, '').replace(/\/+$/, '')
}

const GATEWAY_URL = resolveGatewayUrl()

// Empty values used when the API returns no usable data.
const EMPTY_DASHBOARD: DashboardView = {
  grossRevenue: 0, received: 0, fees: 0, receivable: 0, totalOrders: 0,
  monthlyEvolution: [], platformBreakdown: [], recentOrders: [],
}

const EMPTY_DRE: DreStatement = {
  grossRevenue: 0, fees: 0, taxes: 0, expenses: 0, netProfit: 0, profitMargin: 0, period: '',
}

const BLOCKED_PLATFORMS = new Set(['sandbox'])

function isBlockedPlatform(platform?: string | null): boolean {
  return BLOCKED_PLATFORMS.has((platform ?? '').toLowerCase().trim())
}

function filterReportEntries<T extends { platform?: string | null }>(items: T[] = []): T[] {
  return items.filter((item) => !isBlockedPlatform(item.platform))
}

function toFiniteNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : fallback
  }
  if (typeof value === 'string') {
    const raw = value.trim()
    if (!raw) return fallback
    const normalized = raw.includes(',')
      ? raw.replace(/\./g, '').replace(',', '.')
      : raw
    const parsed = Number(normalized)
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function toOptionalString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value : undefined
}

function toRequiredString(value: unknown, fallback = ''): string {
  return typeof value === 'string' && value.trim() ? value : fallback
}

type RawStockItem = Record<string, unknown>

function normalizeStockItem(item: RawStockItem, tenantId: string): StockItem {
  const sku = toRequiredString(item.sku)
  return {
    id: toRequiredString(item.id, `${tenantId}:${sku}`),
    tenant_id: toRequiredString(item.tenant_id ?? item.tenantId, tenantId),
    sku,
    description: toRequiredString(item.description ?? item.name, sku),
    unit_cost: toFiniteNumber(item.unit_cost ?? item.unitCost),
    quantity: toFiniteNumber(item.quantity ?? item.stockQuantity ?? item.availableQuantity),
    created_at: toOptionalString(item.created_at ?? item.createdAt),
    updated_at: toOptionalString(item.updated_at ?? item.updatedAt),
  }
}

type RawPurchaseEntry = Record<string, unknown>

function normalizePurchaseEntry(entry: RawPurchaseEntry, tenantId: string): PurchaseEntry {
  return {
    id: toRequiredString(entry.id),
    tenant_id: toRequiredString(entry.tenant_id ?? entry.tenantId, tenantId),
    nfe_number: toOptionalString(entry.nfe_number ?? entry.nfeNumber),
    supplier_name: toOptionalString(entry.supplier_name ?? entry.supplierName),
    issue_date: toRequiredString(entry.issue_date ?? entry.issueDate),
    total_cost: toFiniteNumber(entry.total_cost ?? entry.totalCost),
    items: Array.isArray(entry.items) ? entry.items as PurchaseEntry['items'] : [],
    created_at: toOptionalString(entry.created_at ?? entry.createdAt),
  }
}

function emptyReportsSummary(tenantId: string): ReportsSummary {
  return {
    tenant_id: tenantId,
    gross_value: 0,
    received_value: 0,
    fee_value: 0,
    receivable_value: 0,
    entry_count: 0,
  }
}

function summarizeReportEntries(tenantId: string, items: ReportsEntry[]): ReportsSummary {
  return items.reduce<ReportsSummary>(
    (acc, item) => ({
      tenant_id: tenantId,
      gross_value: acc.gross_value + item.gross_value,
      received_value: acc.received_value + item.received_value,
      fee_value: acc.fee_value + item.fee_value,
      receivable_value: acc.receivable_value + item.receivable_value,
      entry_count: acc.entry_count + 1,
    }),
    emptyReportsSummary(tenantId)
  )
}

function subtractReportsSummary(total: ReportsSummary, blocked: ReportsSummary): ReportsSummary {
  return {
    tenant_id: total.tenant_id,
    gross_value: Math.max(0, total.gross_value - blocked.gross_value),
    received_value: Math.max(0, total.received_value - blocked.received_value),
    fee_value: Math.max(0, total.fee_value - blocked.fee_value),
    receivable_value: Math.max(0, total.receivable_value - blocked.receivable_value),
    entry_count: Math.max(0, total.entry_count - blocked.entry_count),
  }
}

// --- API fetch helper ---

async function apiFetch<T>(
  path: string,
  options?: RequestInit,
  token?: string
): Promise<T> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  const requestInit: RequestInit = {
    ...options,
    headers,
  }

  if (options?.cache !== 'no-store') {
    requestInit.next = options?.next ?? { revalidate: 30 }
  }

  const url = `${GATEWAY_URL}${path}`
  const res = await fetch(url, requestInit)
  if (!res.ok) {
    const body = await res.text().catch(() => '')
    throw new Error(
      `API ${res.status} ${res.statusText}: ${url}${body ? ` - ${body.slice(0, 500)}` : ''}`
    )
  }
  return res.json()
}

// ─── Reports-service helpers ──────────────────────────────────────────────────

function mapReportsEntry(e: ReportsEntry): ReportEntry {
  return {
    id: e.id,
    platform: PLATFORM_DISPLAY_REPORTS[e.platform?.toLowerCase()] ?? e.platform,
    orderId: e.order_id,
    saleDate: e.sale_date,
    grossValue: e.gross_value,
    receivedValue: e.received_value,
    feeValue: e.fee_value,
    receivableValue: e.receivable_value,
    paymentMethod: e.payment_method,
    status: e.status,
    releaseDate: e.release_date,
    buyerName: e.buyer_name,
    invoiceNumber: e.invoice_number,
  }
}

const PLATFORM_DISPLAY_REPORTS: Record<string, string> = {
  mercadolivre: 'Mercado Livre', 'mercado-livre': 'Mercado Livre',
  shopee: 'Shopee', amazon: 'Amazon', magalu: 'Magalu',
  bling: 'Bling',
}

function mapReportsDashboardToView(d: ReportsDashboard): DashboardView {
  const entries = filterReportEntries(d.entries?.items ?? [])
  const platformComparison = (d.platform_comparison ?? []).filter((p) => !isBlockedPlatform(p.platform))
  const summary = platformComparison.length > 0
    ? platformComparison.reduce(
        (acc, p) => ({
          tenant_id: d.summary.tenant_id,
          gross_value: acc.gross_value + p.gross_value,
          received_value: acc.received_value + p.received_value,
          fee_value: acc.fee_value + p.fee_value,
          receivable_value: acc.receivable_value + p.receivable_value,
          entry_count: acc.entry_count + p.entry_count,
        }),
        emptyReportsSummary(d.summary.tenant_id)
      )
    : summarizeReportEntries(d.summary.tenant_id, entries)
  const total = platformComparison.reduce((acc, p) => acc + p.gross_value, 0)

  const MONTH_ABBR = ['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez']
  const byMonth = entries.reduce<Record<string, ReportsSummary>>((acc, entry) => {
    const period = entry.sale_date.slice(0, 7)
    const current = acc[period] ?? emptyReportsSummary(d.summary.tenant_id)
    acc[period] = {
      tenant_id: d.summary.tenant_id,
      gross_value: current.gross_value + entry.gross_value,
      received_value: current.received_value + entry.received_value,
      fee_value: current.fee_value + entry.fee_value,
      receivable_value: current.receivable_value + entry.receivable_value,
      entry_count: current.entry_count + 1,
    }
    return acc
  }, {})

  return {
    grossRevenue: summary.gross_value,
    received: summary.received_value,
    fees: summary.fee_value,
    receivable: summary.receivable_value,
    totalOrders: summary.entry_count,
    monthlyEvolution: entries.length > 0 ? Object.entries(byMonth).map(([period, m]) => {
      const monthIdx = parseInt(period.slice(5, 7), 10) - 1
      return {
        month: MONTH_ABBR[monthIdx] ?? period,
        grossRevenue: m.gross_value,
        received: m.received_value,
        fees: m.fee_value,
        netProfit: m.received_value - m.fee_value - m.receivable_value,
      }
    }) : (d.monthly_evolution ?? []).filter(() => platformComparison.length > 0).map((m) => {
      const monthIdx = parseInt(m.period.slice(5, 7), 10) - 1
      return {
        month: MONTH_ABBR[monthIdx] ?? m.period,
        grossRevenue: m.gross_value,
        received: m.received_value,
        fees: m.fee_value,
        netProfit: m.received_value - m.fee_value - m.receivable_value,
      }
    }),
    platformBreakdown: platformComparison.map((p) => ({
      platform: PLATFORM_DISPLAY_REPORTS[p.platform?.toLowerCase()] ?? p.platform,
      amount: p.gross_value,
      percentage: total > 0 ? Math.round((p.gross_value / total) * 100) : 0,
    })),
    recentOrders: entries.map(mapReportsEntry),
  }
}

// --- Data fetching functions ---

export async function getDashboard(token: string, tenantId?: string): Promise<DashboardView> {
  if (tenantId) {
    try {
      const raw = await apiFetch<ReportsDashboard>(
        `/api/reports/tenants/${tenantId}/dashboard`,
        { cache: 'no-store' },
        token
      )
      return mapReportsDashboardToView(raw)
    } catch {
      return EMPTY_DASHBOARD
    }
  }
  return EMPTY_DASHBOARD
}

function mapCoreOrder(order: CoreOrder, displayName: string): ReportEntry {
  const statusMap: Record<string, ReportEntry['status']> = {
    paid: 'PAID',
    pending: 'PENDING',
    cancelled: 'CANCELLED',
    refunded: 'REFUNDED',
  }
  const isPaid = order.status === 'paid'
  return {
    id: order.order_id,
    orderId: order.order_id,
    platform: displayName,
    saleDate: order.date,
    grossValue: order.gross_value,
    feeValue: order.platform_fee,
    receivedValue: isPaid ? order.net_value : 0,
    receivableValue: !isPaid ? order.net_value : 0,
    paymentMethod: order.payment_method,
    releaseDate: order.release_date ?? order.date,
    status: statusMap[order.status] ?? 'PENDING',
    buyerName: order.buyer_name,
  }
}

function thirtyDaysAgo(): string {
  return new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
}

export async function getReportEntries(token: string, tenantId?: string): Promise<ReportEntry[]> {
  if (tenantId) {
    try {
      const since = thirtyDaysAgo()
      const raw = await apiFetch<Paginated<ReportsEntry>>(
        `/api/reports/tenants/${tenantId}/entries?from=${since}&size=100&page=0`,
        { cache: 'no-store' },
        token
      )
      const items = filterReportEntries(raw.items ?? [])
      if (items.length > 0) return items.map(mapReportsEntry)
    } catch { /* fall through */ }
  }

  // Fallback: core-service orders
  try {
    const connectors = await getConnectors(token)
    const active = connectors.filter((c) => c.status === 'connected')
    if (active.length === 0) return []

    const since = thirtyDaysAgo()
    const results = await Promise.allSettled(
      active.map((c) =>
        apiFetch<CoreOrder[]>(
          `/api/core/connectors/${c.name}/orders?from=${since}&limit=100`,
          undefined,
          token
        ).then((orders) => orders.map((o) => mapCoreOrder(o, c.displayName)))
      )
    )
    const entries = filterReportEntries(results.flatMap((r) => (r.status === 'fulfilled' ? r.value : [])))
    if (entries.length > 0) return entries.sort((a, b) => b.saleDate.localeCompare(a.saleDate))
  } catch { /* fall through */ }

  return []
}

export async function getExpenses(token: string, tenantId?: string): Promise<ExpenseEntry[]> {
  if (tenantId) {
    try {
      const raw = await apiFetch<Paginated<ReportsExpense>>(
        `/api/reports/tenants/${tenantId}/expenses?size=100&page=0`,
        { cache: 'no-store' },
        token
      )
      if (raw.items?.length > 0) {
        return raw.items.map((e) => ({
          id: e.id,
          expenseDate: e.expense_date,
          category: e.category,
          description: e.description,
          amount: e.amount,
          attachmentUrl: e.attachment?.secure_url,
        }))
      }
    } catch { /* fall through */ }
  }
  return []
}

export async function getDre(token: string, period: string, tenantId?: string): Promise<DreStatement> {
  if (tenantId) {
    try {
      // period = "2026-06" → from/to desse mês
      const [year, month] = period.split('-')
      const from = `${year}-${month}-01`
      const lastDay = new Date(parseInt(year), parseInt(month), 0).getDate()
      const to = `${year}-${month}-${String(lastDay).padStart(2, '0')}`

      const raw = await apiFetch<ReportsDre>(
        `/api/reports/tenants/${tenantId}/dre?from=${from}&to=${to}`,
        { cache: 'no-store' },
        token
      )
      return {
        grossRevenue: raw.gross_revenue,
        fees: raw.marketplace_fees,
        taxes: raw.estimated_taxes,
        expenses: raw.operating_expenses,
        netProfit: raw.net_result,
        profitMargin: raw.gross_revenue > 0
          ? Math.round((raw.net_result / raw.gross_revenue) * 1000) / 10
          : 0,
        period,
      }
    } catch { /* fall through */ }
  }
  return { ...EMPTY_DRE, period }
}

// ─── Reports-service — funções específicas ────────────────────────────────────

export async function getReportsDashboard(token: string, tenantId: string): Promise<ReportsDashboard | null> {
  try {
    return await apiFetch<ReportsDashboard>(
      `/api/reports/tenants/${tenantId}/dashboard`,
      { cache: 'no-store' },
      token
    )
  } catch { return null }
}

export async function getReportsEntries(
  token: string,
  tenantId: string,
  params: Record<string, string | number> = {}
): Promise<Paginated<ReportsEntry>> {
  if (typeof params.platform === 'string' && isBlockedPlatform(params.platform)) {
    return { items: [], total: 0, page: Number(params.page ?? 0), size: Number(params.size ?? 20) }
  }

  const qs = new URLSearchParams(
    Object.entries({ page: 0, size: 20, ...params }).map(([k, v]) => [k, String(v)])
  ).toString()
  const raw = await apiFetch<Paginated<ReportsEntry>>(
    `/api/reports/tenants/${tenantId}/entries?${qs}`,
    { cache: 'no-store' },
    token
  )
  const items = filterReportEntries(raw.items ?? [])
  return {
    ...raw,
    items,
    total: items.length,
  }
}

export async function getReportsExpenses(
  token: string,
  tenantId: string,
  params: Record<string, string | number> = {}
): Promise<Paginated<ReportsExpense>> {
  const qs = new URLSearchParams(
    Object.entries({ page: 0, size: 20, ...params }).map(([k, v]) => [k, String(v)])
  ).toString()
  return apiFetch<Paginated<ReportsExpense>>(
    `/api/reports/tenants/${tenantId}/expenses?${qs}`,
    { cache: 'no-store' },
    token
  )
}

export async function getReportsDre(token: string, tenantId: string, from: string, to: string): Promise<ReportsDre> {
  return apiFetch<ReportsDre>(
    `/api/reports/tenants/${tenantId}/dre?from=${from}&to=${to}`,
    { cache: 'no-store' },
    token
  )
}

export async function getReportsBalanceSheet(token: string, tenantId: string, asOf: string): Promise<ReportsBalanceSheet | null> {
  try {
    return await apiFetch<ReportsBalanceSheet>(
      `/api/reports/tenants/${tenantId}/balance-sheet?asOf=${asOf}`,
      { cache: 'no-store' },
      token
    )
  } catch { return null }
}

export async function getReportsFilters(token: string, tenantId: string): Promise<ReportsFilters | null> {
  try {
    const filters = await apiFetch<ReportsFilters>(`/api/reports/tenants/${tenantId}/filters`, { cache: 'no-store' }, token)
    return {
      ...filters,
      platforms: (filters.platforms ?? []).filter((platform) => !isBlockedPlatform(platform)),
    }
  } catch { return null }
}

export async function getReportsSummary(
  token: string,
  tenantId: string,
  params: Record<string, string> = {}
): Promise<ReportsSummary | null> {
  if (params.platform && isBlockedPlatform(params.platform)) {
    return emptyReportsSummary(tenantId)
  }

  try {
    const qs = new URLSearchParams(params).toString()
    const summary = await apiFetch<ReportsSummary>(
      `/api/reports/tenants/${tenantId}/summary${qs ? '?' + qs : ''}`,
      { cache: 'no-store' },
      token
    )

    if (params.platform) return summary

    const summariesToBlock = await Promise.all(
      [...BLOCKED_PLATFORMS].map((platform) => {
        const blockedQs = new URLSearchParams({ ...params, platform }).toString()
        return apiFetch<ReportsSummary>(
          `/api/reports/tenants/${tenantId}/summary?${blockedQs}`,
          { cache: 'no-store' },
          token
        ).catch(() => emptyReportsSummary(tenantId))
      })
    )

    return summariesToBlock.reduce(subtractReportsSummary, summary)
  } catch { return emptyReportsSummary(tenantId) }
}

export async function getFiscalProfile(token: string, tenantId: string): Promise<FiscalProfile | null> {
  try {
    return await apiFetch<FiscalProfile>(`/api/reports/tenants/${tenantId}/fiscal-profile`, { cache: 'no-store' }, token)
  } catch { return null }
}

export async function getCloudinarySignature(token: string, tenantId: string): Promise<CloudinaryUploadSignature> {
  return apiFetch<CloudinaryUploadSignature>(
    `/api/reports/tenants/${tenantId}/expenses/upload-signature`,
    { cache: 'no-store' },
    token
  )
}

export async function getMonthlyEvolution(token: string, tenantId: string, from?: string, to?: string): Promise<MonthlyEvolutionItem[]> {
  const qs = [from && `from=${from}`, to && `to=${to}`].filter(Boolean).join('&')
  try {
    return await apiFetch<MonthlyEvolutionItem[]>(
      `/api/reports/tenants/${tenantId}/charts/monthly-evolution${qs ? '?' + qs : ''}`,
      { cache: 'no-store' },
      token
    )
  } catch { return [] }
}

export async function getPlatformComparison(token: string, tenantId: string, from?: string, to?: string): Promise<PlatformComparisonItem[]> {
  const qs = [from && `from=${from}`, to && `to=${to}`].filter(Boolean).join('&')
  try {
    const items = await apiFetch<PlatformComparisonItem[]>(
      `/api/reports/tenants/${tenantId}/charts/platform-comparison${qs ? '?' + qs : ''}`,
      { cache: 'no-store' },
      token
    )
    return items.filter((item) => !isBlockedPlatform(item.platform))
  } catch { return [] }
}

export async function getStockItems(token: string, tenantId: string): Promise<StockItem[]> {
  try {
    const items = await apiFetch<RawStockItem[]>(
      `/api/reports/tenants/${tenantId}/stock/items`,
      { cache: 'no-store' },
      token
    )
    return items.map((item) => normalizeStockItem(item, tenantId))
  } catch { return [] }
}

export async function getPurchaseEntries(token: string, tenantId: string, from?: string, to?: string): Promise<PurchaseEntry[]> {
  const qs = [from && `from=${from}`, to && `to=${to}`].filter(Boolean).join('&')
  try {
    const entries = await apiFetch<RawPurchaseEntry[]>(
      `/api/reports/tenants/${tenantId}/stock/purchases${qs ? '?' + qs : ''}`,
      { cache: 'no-store' },
      token
    )
    return entries.map((entry) => normalizePurchaseEntry(entry, tenantId))
  } catch { return [] }
}

export async function getBankTransactions(token: string, tenantId: string, from?: string, to?: string): Promise<BankTransaction[]> {
  const qs = [from && `from=${from}`, to && `to=${to}`].filter(Boolean).join('&')
  try {
    return await apiFetch<BankTransaction[]>(
      `/api/reports/tenants/${tenantId}/bank/transactions${qs ? '?' + qs : ''}`,
      { cache: 'no-store' },
      token
    )
  } catch { return [] }
}

export async function getInvoices(token: string, tenantId: string, from?: string, to?: string): Promise<InvoiceEntry[]> {
  const qs = [from && `from=${from}`, to && `to=${to}`].filter(Boolean).join('&')
  try {
    return await apiFetch<InvoiceEntry[]>(
      `/api/reports/tenants/${tenantId}/invoices${qs ? '?' + qs : ''}`,
      { cache: 'no-store' },
      token
    )
  } catch { return [] }
}

export async function getUnmatchedInvoices(token: string, tenantId: string, from: string, to: string): Promise<InvoiceEntry[]> {
  try {
    return await apiFetch<InvoiceEntry[]>(
      `/api/reports/tenants/${tenantId}/invoices/unmatched?from=${from}&to=${to}`,
      { cache: 'no-store' },
      token
    )
  } catch { return [] }
}

export async function getAccountingClosing(token: string, tenantId: string, month: string): Promise<AccountingClosing | null> {
  try {
    return await apiFetch<AccountingClosing>(`/api/reports/tenants/${tenantId}/closings/${month}`, { cache: 'no-store' }, token)
  } catch { return null }
}

export async function getProfitAvailability(token: string, tenantId: string): Promise<ProfitAvailability | null> {
  try {
    return await apiFetch<ProfitAvailability>(
      `/api/reports/tenants/${tenantId}/profit/available`,
      { cache: 'no-store' },
      token
    )
  } catch { return null }
}

export async function getProfitDistributions(token: string, tenantId: string, month?: string): Promise<ProfitDistribution[]> {
  const qs = month ? `?month=${encodeURIComponent(month)}` : ''
  try {
    return await apiFetch<ProfitDistribution[]>(
      `/api/reports/tenants/${tenantId}/profit/distributions${qs}`,
      { cache: 'no-store' },
      token
    )
  } catch { return [] }
}

export function getExportUrl(tenantId: string, format: 'pdf' | 'xlsx' | 'csv', month?: string, platform?: string): string {
  const base = `${GATEWAY_URL}/api/reports/tenants/${tenantId}/exports`
  if (platform) return `${base}/platforms/${platform}?format=${format}`
  return `${base}/monthly?format=${format}&month=${month ?? ''}`
}

const PLATFORM_DISPLAY: Record<string, string> = {
  mercadolivre:  'Mercado Livre',
  'mercado-livre': 'Mercado Livre',
  mercadolibre:  'Mercado Livre',
  shopee:        'Shopee',
  amazon:        'Amazon',
  magalu:        'Magalu',
  bling:         'Bling',
}

function coreStatusToUi(status: CoreConnector['status']): ConnectorStatus['status'] {
  if (status === 'active') return 'connected'
  if (status === 'error') return 'error'
  if (status === 'syncing') return 'syncing'
  return 'disconnected'
}

export async function getConnectors(token: string): Promise<ConnectorStatus[]> {
  try {
    const raw = await apiFetch<Record<string, unknown>[]>(
      '/api/core/connectors',
      { cache: 'no-store' },
      token
    )

    // Para cada conector, busca o status real em paralelo via /status
    const results = await Promise.allSettled(
      raw.map(async (c): Promise<ConnectorStatus | null> => {
        const name = ((c.platform ?? c.connector_name ?? c.name ?? '') as string)
          .toLowerCase()
          .trim()
        if (!name) return null

        const displayName = PLATFORM_DISPLAY[name] ?? (c.platform as string) ?? name

        try {
          const live = await apiFetch<{
            platform?: string
            status: string
            message?: string
            checked_at?: string
          }>(`/api/core/connectors/${name}/status`, { cache: 'no-store' }, token)

          return {
            name,
            displayName,
            status: coreStatusToUi((live.status as CoreConnector['status']) ?? 'inactive'),
            lastSync: live.checked_at,
          } satisfies ConnectorStatus
        } catch {
          // Fallback para o status da lista se /status falhar
          return {
            name,
            displayName,
            status: coreStatusToUi((c.status as CoreConnector['status']) ?? 'inactive'),
            lastSync: (c.checked_at ?? c.last_sync) as string | undefined,
          } satisfies ConnectorStatus
        }
      })
    )

    return results
      .map((r) => (r.status === 'fulfilled' ? r.value : null))
      .filter((c): c is ConnectorStatus => c !== null && c.name !== 'sandbox')
  } catch {
    return []
  }
}

export async function getIntegrationsHealth(token: string): Promise<IntegrationHealthSummary[]> {
  try {
    return await apiFetch<IntegrationHealthSummary[]>(
      '/api/core/integrations/health',
      { cache: 'no-store' },
      token
    )
  } catch { return [] }
}

export async function getIntegrationLogs(token: string, integrationName: string, severity?: string): Promise<IntegrationEventLog[]> {
  const qs = severity ? `?severity=${encodeURIComponent(severity)}` : ''
  try {
    return await apiFetch<IntegrationEventLog[]>(
      `/api/core/integrations/${encodeURIComponent(integrationName)}/logs${qs}`,
      { cache: 'no-store' },
      token
    )
  } catch { return [] }
}

export async function syncConnector(token: string, connectorName: string, since?: string): Promise<string> {
  const res = await fetch(`${GATEWAY_URL}/api/core/connectors/${connectorName}/sync-all`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      since: since ?? new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
    }),
    cache: 'no-store',
  })

  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as Record<string, unknown>
    throw new Error((body.message as string) ?? `API ${res.status}`)
  }

  // O endpoint retorna o jobId como string JSON ("uuid") ou plain text (uuid)
  const text = await res.text()
  try {
    const parsed = JSON.parse(text)
    return typeof parsed === 'string' ? parsed : String(parsed)
  } catch {
    return text.trim()
  }
}

export async function getSyncJob(token: string, jobId: string): Promise<SyncJob> {
  return apiFetch<SyncJob>(`/api/core/connectors/sync-jobs/${jobId}`, undefined, token)
}

export async function getNotifications(token: string, tenantId?: string): Promise<NotificationMessage[]> {
  if (!tenantId) return []
  try {
    return await apiFetch<NotificationMessage[]>(
      `/api/notifications/tenants/${tenantId}?limit=50`,
      { cache: 'no-store' },
      token
    )
  } catch {
    return []
  }
}

export async function getNotificationPreferences(token: string, tenantId: string): Promise<NotificationPreferences | null> {
  try {
    return await apiFetch<NotificationPreferences>(
      `/api/notifications/tenants/${tenantId}/preferences`,
      { cache: 'no-store' },
      token
    )
  } catch {
    return null
  }
}

export async function getBilling(_token: string): Promise<BillingSubscription | null> {
  try {
    return await apiFetch<BillingSubscription>('/api/billing/subscription', undefined, _token)
  } catch {
    return null
  }
}

export async function getUser(_token: string): Promise<UserSession | null> {
  try {
    // user-service pode retornar { id, tenantId, email, fullName, roles }
    const raw = await apiFetch<Record<string, unknown>>('/api/users/me', undefined, _token)
    return {
      tenantId: (raw.tenantId ?? raw.tenant_id ?? '') as string,
      userId: (raw.userId ?? raw.user_id ?? raw.id ?? '') as string,
      email: (raw.email ?? '') as string,
      fullName: (raw.fullName ?? raw.full_name ?? raw.name ?? '') as string,
      roles: Array.isArray(raw.roles) ? (raw.roles as string[]) : [],
    }
  } catch {
    return null
  }
}

export async function getAccountants(token: string, tenantId: string): Promise<UserProfile[]> {
  try {
    const members = await apiFetch<UserProfile[]>(
      `/api/users/tenants/${tenantId}/members`,
      undefined,
      token
    )
    return members.filter((m) => m.roles.includes('CONTADOR'))
  } catch {
    return []
  }
}

export async function getAccountantClients(token: string): Promise<AccountantClient[]> {
  try {
    const clients = await apiFetch<AccountantClient[]>(
      '/api/users/accountant/clients',
      { cache: 'no-store' },
      token
    )
    return clients.map((client) => ({
      tenantId: toRequiredString(client.tenantId),
      legalName: toRequiredString(client.legalName),
      tradeName: toOptionalString(client.tradeName),
      tenantStatus: toRequiredString(client.tenantStatus, 'ACTIVE'),
      readOnly: Boolean(client.readOnly),
      accessStatus: toRequiredString(client.accessStatus, 'ACTIVE'),
      grantedAt: toOptionalString(client.grantedAt),
    }))
  } catch {
    return []
  }
}

export async function getCurrentUser(token: string, tenantId: string, email: string): Promise<UserProfile | null> {
  try {
    const members = await apiFetch<UserProfile[]>(
      `/api/users/tenants/${tenantId}/members`,
      undefined,
      token
    )
    const me = members.find((m) => m.email === email) ?? null
    if (me && !me.fullName && (me.firstName || me.lastName)) {
      me.fullName = [me.firstName, me.lastName].filter(Boolean).join(' ')
    }
    return me
  } catch {
    return null
  }
}

export function formatCurrency(value: unknown): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(toFiniteNumber(value))
}

export function formatDate(dateStr: string): string {
  return new Intl.DateTimeFormat('pt-BR').format(new Date(dateStr))
}
