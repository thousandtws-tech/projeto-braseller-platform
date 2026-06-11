import { Search, Download, ChevronLeft, ChevronRight, FileText } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { DatePicker } from '@/shared/ui/date-picker'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select'
import { getToken, getSession } from '@/entities/session/server/session'
import {
  getReportsEntries,
  getReportsFilters,
  getReportsSummary,
  getExportUrl,
  formatCurrency,
  formatDate,
} from '@/shared/api/gateway'
import { getDictionary, type Dictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import type { ReportsEntry } from '@/shared/types'

const STATUS_VARIANT: Record<string, 'success' | 'warning' | 'destructive' | 'secondary'> = {
  PAID: 'success',
  PENDING: 'warning',
  CANCELLED: 'destructive',
  REFUNDED: 'secondary',
}

const PAGE_SIZE = 20

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{
    platform?: string
    status?: string
    paymentMethod?: string
    search?: string
    from?: string
    to?: string
    page?: string
  }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.sales.title }
}

export default async function LancamentosPage({ params, searchParams }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const sp = await searchParams
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId ?? ''

  const page = Math.max(0, parseInt(sp.page ?? '0', 10))

  const filterParams: Record<string, string> = {}
  if (sp.platform)      filterParams.platform      = sp.platform
  if (sp.status)        filterParams.status        = sp.status
  if (sp.paymentMethod) filterParams.paymentMethod = sp.paymentMethod
  if (sp.search)        filterParams.search        = sp.search
  if (sp.from)          filterParams.from          = sp.from
  if (sp.to)            filterParams.to            = sp.to

  const [paginated, filters, summary] = await Promise.all([
    tenantId
      ? getReportsEntries(token, tenantId, { ...filterParams, page, size: PAGE_SIZE })
          .catch(() => ({ items: [] as ReportsEntry[], total: 0, page: 0, size: PAGE_SIZE }))
      : Promise.resolve({ items: [] as ReportsEntry[], total: 0, page: 0, size: PAGE_SIZE }),
    tenantId ? getReportsFilters(token, tenantId) : Promise.resolve(null),
    tenantId ? getReportsSummary(token, tenantId, filterParams) : Promise.resolve(null),
  ])

  const totalPages = Math.ceil((paginated.total || 0) / PAGE_SIZE)
  const hasNext = page < totalPages - 1
  const hasPrev = page > 0

  function buildUrl(overrides: Record<string, string | number>) {
    const params = new URLSearchParams({
      ...filterParams,
      page: String(page),
      ...Object.fromEntries(Object.entries(overrides).map(([k, v]) => [k, String(v)])),
    })
    return `?${params.toString()}`
  }

  const exportUrl = tenantId
    ? getExportUrl(tenantId, 'xlsx', new Date().toISOString().slice(0, 7))
    : '#'

  return (
    <div className="space-y-5 max-w-7xl">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-semibold">{dict.sales.title}</h2>
          <p className="text-sm text-muted-foreground">
            {paginated.total > 0
              ? formatMessage(dict.sales.ordersFound, { count: paginated.total })
              : dict.sales.noOrdersFound}
          </p>
        </div>
        <a
          href={exportUrl}
          className="inline-flex items-center gap-1.5 h-7 px-2.5 rounded-[min(var(--radius-md),12px)] border border-border bg-background text-[0.8rem] font-medium transition-colors hover:bg-muted"
        >
          <Download className="size-3.5" />
          {dict.sales.exportXlsx}
        </a>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {[
          { label: dict.sales.kpis.invoiced,    value: summary?.gross_value ?? 0,     color: 'text-foreground' },
          { label: dict.sales.kpis.received,    value: summary?.received_value ?? 0,  color: 'text-emerald-600 dark:text-emerald-400' },
          { label: dict.sales.kpis.feesShipping,value: summary?.fee_value ?? 0,       color: 'text-destructive' },
          { label: dict.sales.kpis.receivable,  value: summary?.receivable_value ?? 0,color: 'text-amber-600 dark:text-amber-400' },
        ].map((s) => (
          <div key={s.label} className="rounded-xl border border-border bg-card p-4">
            <p className="text-xs text-muted-foreground">{s.label}</p>
            <p className={`text-lg font-semibold mt-1 tabular-nums ${s.color}`}>
              {formatCurrency(s.value)}
            </p>
          </div>
        ))}
      </div>

      {/* Filters — form GET */}
      <form method="GET" className="flex items-center gap-2 flex-wrap">
        {/* Date range */}
        <DatePicker
          name="from"
          defaultValue={sp.from ?? ''}
          placeholder={dict.sales.filters.dateFrom}
          buttonClassName="h-8"
        />
        <span className="text-muted-foreground text-sm">{dict.sales.filters.to}</span>
        <DatePicker
          name="to"
          defaultValue={sp.to ?? ''}
          placeholder={dict.sales.filters.dateTo}
          buttonClassName="h-8"
        />

        {/* Platform */}
        <Select
          name="platform"
          defaultValue={sp.platform ?? ''}
        >
          <SelectTrigger className="w-48">
            <SelectValue placeholder={dict.sales.filters.allPlatforms} />
          </SelectTrigger>
          <SelectContent align="start">
            <SelectGroup>
              <SelectItem value="">{dict.sales.filters.allPlatforms}</SelectItem>
              {(filters?.platforms ?? []).map((p) => (
                <SelectItem key={p} value={p}>{p}</SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>

        {/* Status */}
        <Select
          name="status"
          defaultValue={sp.status ?? ''}
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder={dict.sales.filters.allStatuses} />
          </SelectTrigger>
          <SelectContent align="start">
            <SelectGroup>
              <SelectItem value="">{dict.sales.filters.allStatuses}</SelectItem>
              {(filters?.statuses ?? ['PAID', 'PENDING', 'CANCELLED', 'REFUNDED']).map((s) => (
                <SelectItem key={s} value={s}>{dict.dashboard.status[s as keyof typeof dict.dashboard.status] ?? s}</SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>

        {/* Payment method */}
        <Select
          name="paymentMethod"
          defaultValue={sp.paymentMethod ?? ''}
        >
          <SelectTrigger className="w-48">
            <SelectValue placeholder={dict.sales.filters.allPayments} />
          </SelectTrigger>
          <SelectContent align="start">
            <SelectGroup>
              <SelectItem value="">{dict.sales.filters.allPayments}</SelectItem>
              {(filters?.payment_methods ?? []).map((m) => (
                <SelectItem key={m} value={m}>{dict.sales.paymentMethods[m as keyof typeof dict.sales.paymentMethods] ?? m}</SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>

        {/* Search */}
        <div className="relative flex-1 min-w-52">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-3.5 text-muted-foreground" />
          <input
            name="search"
            type="text"
            defaultValue={sp.search ?? ''}
            placeholder={dict.sales.filters.searchPlaceholder}
            className="h-8 w-full rounded-lg border border-input bg-background pl-8 pr-3 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        <Button type="submit" size="sm">{dict.sales.filters.filter}</Button>
        {Object.keys(filterParams).length > 0 && (
          <a href={`/${lang}/lancamentos`} className="text-xs text-muted-foreground hover:text-foreground transition-colors">
            {dict.sales.filters.clear}
          </a>
        )}
      </form>

      {/* Table */}
      <Card>
        <CardHeader className="flex-row items-center justify-between pb-2">
          <CardTitle>{dict.sales.table.title}</CardTitle>
          {paginated.total > 0 && (
            <span className="text-xs text-muted-foreground">
              {formatMessage(dict.sales.table.showingRange, {
                from: page * PAGE_SIZE + 1,
                to: Math.min((page + 1) * PAGE_SIZE, paginated.total),
                total: paginated.total,
              })}
            </span>
          )}
        </CardHeader>
        <CardContent className="p-0">
          {paginated.items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3 text-center">
              <FileText className="size-10 text-muted-foreground/30" />
              <div>
                <p className="text-sm font-medium">{dict.sales.table.empty.title}</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {dict.sales.table.empty.hint}
                </p>
              </div>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    {Object.values(dict.sales.table.columns).map((h) => (
                      <th key={h} className="text-left px-4 py-3 text-xs font-medium text-muted-foreground whitespace-nowrap">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {paginated.items.map((entry) => (
                    <EntryRow key={entry.id} entry={entry} dict={dict} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-xs text-muted-foreground">
            {formatMessage(dict.sales.pagination.page, { current: page + 1, total: totalPages })}
          </span>
          <div className="flex items-center gap-2">
            <a
              href={hasPrev ? buildUrl({ page: page - 1 }) : '#'}
              aria-disabled={!hasPrev}
              className={`inline-flex items-center gap-1 h-8 px-3 rounded-lg border text-sm transition-colors ${
                hasPrev
                  ? 'border-input bg-background hover:bg-muted'
                  : 'border-border/50 bg-muted/30 text-muted-foreground pointer-events-none opacity-50'
              }`}
            >
              <ChevronLeft className="size-3.5" /> {dict.sales.pagination.previous}
            </a>
            <a
              href={hasNext ? buildUrl({ page: page + 1 }) : '#'}
              aria-disabled={!hasNext}
              className={`inline-flex items-center gap-1 h-8 px-3 rounded-lg border text-sm transition-colors ${
                hasNext
                  ? 'border-input bg-background hover:bg-muted'
                  : 'border-border/50 bg-muted/30 text-muted-foreground pointer-events-none opacity-50'
              }`}
            >
              {dict.sales.pagination.next} <ChevronRight className="size-3.5" />
            </a>
          </div>
        </div>
      )}
    </div>
  )
}

function EntryRow({ entry, dict }: { entry: ReportsEntry; dict: Dictionary }) {
  const label = dict.dashboard.status[entry.status as keyof typeof dict.dashboard.status] ?? entry.status
  const variant = STATUS_VARIANT[entry.status] ?? 'secondary'
  const net = entry.received_value > 0 ? entry.received_value : entry.receivable_value

  return (
    <tr className="border-b border-border/50 hover:bg-muted/30 transition-colors">
      <td className="px-4 py-3 font-mono text-xs text-muted-foreground whitespace-nowrap">{entry.order_id}</td>
      <td className="px-4 py-3 whitespace-nowrap text-muted-foreground text-xs">{formatDate(entry.sale_date)}</td>
      <td className="px-4 py-3 font-medium whitespace-nowrap max-w-[160px] truncate">{entry.buyer_name}</td>
      <td className="px-4 py-3 whitespace-nowrap text-sm">{entry.platform}</td>
      <td className="px-4 py-3 whitespace-nowrap text-xs text-muted-foreground">{dict.sales.paymentMethods[entry.payment_method as keyof typeof dict.sales.paymentMethods] ?? entry.payment_method}</td>
      <td className="px-4 py-3 whitespace-nowrap font-medium tabular-nums">{formatCurrency(entry.gross_value)}</td>
      <td className="px-4 py-3 whitespace-nowrap text-destructive tabular-nums">-{formatCurrency(entry.fee_value)}</td>
      <td className={`px-4 py-3 whitespace-nowrap font-medium tabular-nums ${net > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-amber-600 dark:text-amber-400'}`}>
        {formatCurrency(net)}
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-xs text-muted-foreground">{formatDate(entry.release_date)}</td>
      <td className="px-4 py-3 whitespace-nowrap"><Badge variant={variant} className="text-xs">{label}</Badge></td>
    </tr>
  )
}
