import type { Metadata } from 'next'
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
import type { ReportsEntry } from '@/shared/types'

export const metadata: Metadata = { title: 'Lançamentos' }

const STATUS_MAP: Record<string, { label: string; variant: 'success' | 'warning' | 'destructive' | 'secondary' }> = {
  PAID:      { label: 'Pago',        variant: 'success' },
  PENDING:   { label: 'Pendente',    variant: 'warning' },
  CANCELLED: { label: 'Cancelado',   variant: 'destructive' },
  REFUNDED:  { label: 'Reembolsado', variant: 'secondary' },
}

const PAYMENT_LABELS: Record<string, string> = {
  PIX: 'PIX', CREDIT_CARD: 'Cartão Crédito', DEBIT_CARD: 'Cartão Débito',
  BOLETO: 'Boleto', INSTALLMENT: 'Parcelado',
}

const PAGE_SIZE = 20

interface Props {
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

export default async function LancamentosPage({ searchParams }: Props) {
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
          <h2 className="text-xl font-semibold">Lançamentos</h2>
          <p className="text-sm text-muted-foreground">
            {paginated.total > 0
              ? `${paginated.total} pedidos encontrados`
              : 'Nenhum pedido encontrado'}
          </p>
        </div>
        <a
          href={exportUrl}
          className="inline-flex items-center gap-1.5 h-7 px-2.5 rounded-[min(var(--radius-md),12px)] border border-border bg-background text-[0.8rem] font-medium transition-colors hover:bg-muted"
        >
          <Download className="size-3.5" />
          Exportar XLSX
        </a>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {[
          { label: 'Faturado',      value: summary?.gross_value ?? 0,     color: 'text-foreground' },
          { label: 'Recebido',      value: summary?.received_value ?? 0,  color: 'text-emerald-600 dark:text-emerald-400' },
          { label: 'Taxas/Frete',   value: summary?.fee_value ?? 0,       color: 'text-destructive' },
          { label: 'A Receber',     value: summary?.receivable_value ?? 0,color: 'text-amber-600 dark:text-amber-400' },
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
          placeholder="Data inicial"
          buttonClassName="h-8"
        />
        <span className="text-muted-foreground text-sm">até</span>
        <DatePicker
          name="to"
          defaultValue={sp.to ?? ''}
          placeholder="Data final"
          buttonClassName="h-8"
        />

        {/* Platform */}
        <Select
          name="platform"
          defaultValue={sp.platform ?? ''}
        >
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Todas as plataformas" />
          </SelectTrigger>
          <SelectContent align="start">
            <SelectGroup>
              <SelectItem value="">Todas as plataformas</SelectItem>
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
            <SelectValue placeholder="Todos os status" />
          </SelectTrigger>
          <SelectContent align="start">
            <SelectGroup>
              <SelectItem value="">Todos os status</SelectItem>
              {(filters?.statuses ?? ['PAID', 'PENDING', 'CANCELLED', 'REFUNDED']).map((s) => (
                <SelectItem key={s} value={s}>{STATUS_MAP[s]?.label ?? s}</SelectItem>
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
            <SelectValue placeholder="Todos os pagamentos" />
          </SelectTrigger>
          <SelectContent align="start">
            <SelectGroup>
              <SelectItem value="">Todos os pagamentos</SelectItem>
              {(filters?.payment_methods ?? []).map((m) => (
                <SelectItem key={m} value={m}>{PAYMENT_LABELS[m] ?? m}</SelectItem>
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
            placeholder="Buscar pedido ou comprador..."
            className="h-8 w-full rounded-lg border border-input bg-background pl-8 pr-3 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        <Button type="submit" size="sm">Filtrar</Button>
        {Object.keys(filterParams).length > 0 && (
          <a href="/lancamentos" className="text-xs text-muted-foreground hover:text-foreground transition-colors">
            Limpar
          </a>
        )}
      </form>

      {/* Table */}
      <Card>
        <CardHeader className="flex-row items-center justify-between pb-2">
          <CardTitle>Pedidos</CardTitle>
          {paginated.total > 0 && (
            <span className="text-xs text-muted-foreground">
              {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, paginated.total)} de {paginated.total}
            </span>
          )}
        </CardHeader>
        <CardContent className="p-0">
          {paginated.items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3 text-center">
              <FileText className="size-10 text-muted-foreground/30" />
              <div>
                <p className="text-sm font-medium">Nenhum lançamento encontrado</p>
                <p className="text-xs text-muted-foreground mt-1">
                  Ajuste os filtros ou sincronize os conectores.
                </p>
              </div>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    {['Pedido', 'Data', 'Comprador', 'Plataforma', 'Pagamento', 'Bruto', 'Taxa', 'Líquido', 'Liberação', 'Status'].map((h) => (
                      <th key={h} className="text-left px-4 py-3 text-xs font-medium text-muted-foreground whitespace-nowrap">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {paginated.items.map((entry) => (
                    <EntryRow key={entry.id} entry={entry} />
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
            Página {page + 1} de {totalPages}
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
              <ChevronLeft className="size-3.5" /> Anterior
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
              Próxima <ChevronRight className="size-3.5" />
            </a>
          </div>
        </div>
      )}
    </div>
  )
}

function EntryRow({ entry }: { entry: ReportsEntry }) {
  const { label, variant } = STATUS_MAP[entry.status] ?? { label: entry.status, variant: 'secondary' as const }
  const net = entry.received_value > 0 ? entry.received_value : entry.receivable_value

  return (
    <tr className="border-b border-border/50 hover:bg-muted/30 transition-colors">
      <td className="px-4 py-3 font-mono text-xs text-muted-foreground whitespace-nowrap">{entry.order_id}</td>
      <td className="px-4 py-3 whitespace-nowrap text-muted-foreground text-xs">{formatDate(entry.sale_date)}</td>
      <td className="px-4 py-3 font-medium whitespace-nowrap max-w-[160px] truncate">{entry.buyer_name}</td>
      <td className="px-4 py-3 whitespace-nowrap text-sm">{entry.platform}</td>
      <td className="px-4 py-3 whitespace-nowrap text-xs text-muted-foreground">{PAYMENT_LABELS[entry.payment_method] ?? entry.payment_method}</td>
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
