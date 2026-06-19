import Link from 'next/link'
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  CircleDollarSign,
  Download,
  FileText,
  Filter,
  ReceiptText,
  Search,
  WalletCards,
  X,
} from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { DatePicker } from '@/shared/ui/date-picker'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/ui/table'
import { getToken, getSession } from '@/entities/session/server/session'
import {
  formatCurrency,
  formatDate,
  getExportUrl,
  getReportsEntries,
  getReportsFilters,
  getReportsSummary,
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
  const [dict, sp, token, session] = await Promise.all([
    getDictionary(lang),
    searchParams,
    getToken().then((value) => value ?? ''),
    getSession(),
  ])
  const tenantId = session?.tenantId ?? ''
  const page = Math.max(0, parseInt(sp.page ?? '0', 10))

  const filterParams: Record<string, string> = {}
  if (sp.platform) filterParams.platform = sp.platform
  if (sp.status) filterParams.status = sp.status
  if (sp.paymentMethod) filterParams.paymentMethod = sp.paymentMethod
  if (sp.search) filterParams.search = sp.search
  if (sp.from) filterParams.from = sp.from
  if (sp.to) filterParams.to = sp.to

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
  const activeFilters = getActiveFilters(sp, dict)
  const exportUrl = tenantId
    ? getExportUrl(tenantId, 'xlsx', new Date().toISOString().slice(0, 7))
    : '#'

  function buildUrl(overrides: Record<string, string | number | undefined>) {
    const next = new URLSearchParams({
      ...filterParams,
      page: String(page),
    })
    Object.entries(overrides).forEach(([key, value]) => {
      if (value === undefined || value === '') next.delete(key)
      else next.set(key, String(value))
    })
    return `?${next.toString()}`
  }

  return (
    <div className="flex w-full flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.sales.title}</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Acompanhe valores, repasses e situação de cada pedido.
          </p>
        </div>
        <a
          href={exportUrl}
          aria-disabled={!tenantId}
          className="inline-flex h-10 items-center gap-2 rounded-md border border-input bg-background px-3.5 text-sm font-medium transition hover:border-foreground/30 hover:bg-muted aria-disabled:pointer-events-none aria-disabled:opacity-50"
        >
          <Download className="size-4" />
          {dict.sales.exportXlsx}
        </a>
      </header>

      <MetricsRail summary={summary} dict={dict} />

      <section className="rounded-lg border border-border bg-card">
        <div className="flex flex-col gap-4 border-b border-border p-4 lg:flex-row lg:items-center">
          <form method="GET" className="flex min-w-0 flex-1 flex-col gap-3 lg:flex-row lg:items-center">
            <div className="relative min-w-0 flex-1">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <input
                name="search"
                type="search"
                defaultValue={sp.search ?? ''}
                placeholder={dict.sales.filters.searchPlaceholder}
                className="h-10 w-full rounded-md border border-input bg-background pl-10 pr-3 text-sm outline-none transition placeholder:text-muted-foreground focus:border-foreground/40 focus:ring-2 focus:ring-ring/15"
              />
            </div>
            <Button type="submit" size="lg" className="lg:px-5">
              <Search />
              Buscar
            </Button>
          </form>
          <div className="hidden h-7 w-px bg-border lg:block" />
          <span className="whitespace-nowrap text-xs text-muted-foreground">
            {paginated.total > 0
              ? formatMessage(dict.sales.ordersFound, { count: paginated.total })
              : dict.sales.noOrdersFound}
          </span>
        </div>

        <form method="GET" className="flex flex-col gap-4 p-4">
          {sp.search ? <input type="hidden" name="search" value={sp.search} /> : null}
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-muted-foreground" />
            <p className="text-xs font-semibold uppercase tracking-[0.08em]">Refinar resultados</p>
            {activeFilters.length > 0 ? (
              <Badge variant="secondary">{activeFilters.length}</Badge>
            ) : null}
          </div>

          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[1fr_1fr_1.2fr_1fr_1.2fr_auto]">
            <DatePicker
              name="from"
              defaultValue={sp.from ?? ''}
              placeholder={dict.sales.filters.dateFrom}
              buttonClassName="h-10"
              className="min-w-0"
            />
            <DatePicker
              name="to"
              defaultValue={sp.to ?? ''}
              placeholder={dict.sales.filters.dateTo}
              buttonClassName="h-10"
              className="min-w-0"
            />
            <FilterSelect
              name="platform"
              defaultValue={sp.platform ?? ''}
              placeholder={dict.sales.filters.allPlatforms}
              items={(filters?.platforms ?? []).map((value) => ({ value, label: value }))}
            />
            <FilterSelect
              name="status"
              defaultValue={sp.status ?? ''}
              placeholder={dict.sales.filters.allStatuses}
              items={(filters?.statuses ?? ['PAID', 'PENDING', 'CANCELLED', 'REFUNDED']).map((value) => ({
                value,
                label: dict.dashboard.status[value as keyof typeof dict.dashboard.status] ?? value,
              }))}
            />
            <FilterSelect
              name="paymentMethod"
              defaultValue={sp.paymentMethod ?? ''}
              placeholder={dict.sales.filters.allPayments}
              items={(filters?.payment_methods ?? []).map((value) => ({
                value,
                label: dict.sales.paymentMethods[value as keyof typeof dict.sales.paymentMethods] ?? value,
              }))}
            />
            <Button type="submit" size="lg" variant="outline">
              Aplicar
            </Button>
          </div>

          {activeFilters.length > 0 ? (
            <div className="flex flex-wrap items-center gap-2 border-t border-border pt-4">
              <span className="mr-1 text-xs text-muted-foreground">Filtros ativos:</span>
              {activeFilters.map((filter) => (
                <Link
                  key={filter.key}
                  href={buildUrl({ [filter.key]: undefined, page: 0 })}
                  className="inline-flex h-7 items-center gap-1.5 rounded-md border border-border bg-muted/45 px-2.5 text-xs text-foreground transition hover:bg-muted"
                >
                  {filter.label}
                  <X className="size-3" />
                </Link>
              ))}
              <Link
                href={`/${lang}/lancamentos`}
                className="ml-auto text-xs font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
              >
                {dict.sales.filters.clear}
              </Link>
            </div>
          ) : null}
        </form>
      </section>

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between gap-4">
          <div>
            <CardTitle>{dict.sales.table.title}</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">
              Valores brutos, custos e repasses consolidados.
            </p>
          </div>
          {paginated.total > 0 ? (
            <span className="text-xs text-muted-foreground">
              {formatMessage(dict.sales.table.showingRange, {
                from: page * PAGE_SIZE + 1,
                to: Math.min((page + 1) * PAGE_SIZE, paginated.total),
                total: paginated.total,
              })}
            </span>
          ) : null}
        </CardHeader>
        <CardContent className="p-0">
          {paginated.items.length === 0 ? (
            <EmptyState hasFilters={activeFilters.length > 0 || Boolean(sp.search)} lang={lang} dict={dict} />
          ) : (
            <Table>
              <TableHeader className="sticky top-0 bg-muted/70">
                <TableRow className="hover:bg-muted/70">
                  <TableHead className="pl-5">{dict.sales.table.columns.order}</TableHead>
                  <TableHead>{dict.sales.table.columns.buyer}</TableHead>
                  <TableHead>{dict.sales.table.columns.platform}</TableHead>
                  <TableHead>{dict.sales.table.columns.date}</TableHead>
                  <TableHead>{dict.sales.table.columns.payment}</TableHead>
                  <TableHead className="text-right">{dict.sales.table.columns.gross}</TableHead>
                  <TableHead className="text-right">{dict.sales.table.columns.fee}</TableHead>
                  <TableHead className="text-right">{dict.sales.table.columns.net}</TableHead>
                  <TableHead>{dict.sales.table.columns.release}</TableHead>
                  <TableHead className="pr-5">{dict.sales.table.columns.status}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginated.items.map((entry) => (
                  <EntryRow key={entry.id} entry={entry} dict={dict} />
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {totalPages > 1 ? (
        <nav className="flex flex-wrap items-center justify-between gap-3" aria-label="Paginação">
          <p className="text-xs text-muted-foreground">
            {formatMessage(dict.sales.pagination.page, { current: page + 1, total: totalPages })}
          </p>
          <div className="flex items-center gap-2">
            <PaginationLink
              href={hasPrev ? buildUrl({ page: page - 1 }) : '#'}
              disabled={!hasPrev}
              label={dict.sales.pagination.previous}
              icon="previous"
            />
            <span className="flex size-9 items-center justify-center rounded-md bg-foreground text-xs font-semibold text-background">
              {page + 1}
            </span>
            <PaginationLink
              href={hasNext ? buildUrl({ page: page + 1 }) : '#'}
              disabled={!hasNext}
              label={dict.sales.pagination.next}
              icon="next"
            />
          </div>
        </nav>
      ) : null}
    </div>
  )
}

function MetricsRail({
  summary,
  dict,
}: {
  summary: Awaited<ReturnType<typeof getReportsSummary>>
  dict: Dictionary
}) {
  const metrics = [
    {
      label: dict.sales.kpis.invoiced,
      value: summary?.gross_value ?? 0,
      helper: 'Valor total dos pedidos',
      icon: ReceiptText,
    },
    {
      label: dict.sales.kpis.received,
      value: summary?.received_value ?? 0,
      helper: 'Já disponível em conta',
      icon: ArrowDown,
    },
    {
      label: dict.sales.kpis.feesShipping,
      value: summary?.fee_value ?? 0,
      helper: 'Custos descontados',
      icon: ArrowUp,
    },
    {
      label: dict.sales.kpis.receivable,
      value: summary?.receivable_value ?? 0,
      helper: 'Repasses futuros',
      icon: WalletCards,
    },
  ]

  return (
    <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
      {metrics.map((metric) => (
        <div
          key={metric.label}
          className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0"
        >
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs text-muted-foreground">{metric.label}</span>
            <metric.icon className="size-4 text-muted-foreground" />
          </div>
          <p className="text-2xl font-semibold tracking-[-0.035em] tabular-nums">
            {formatCurrency(metric.value)}
          </p>
          <p className="text-[11px] text-muted-foreground">{metric.helper}</p>
        </div>
      ))}
    </section>
  )
}

function FilterSelect({
  name,
  defaultValue,
  placeholder,
  items,
}: {
  name: string
  defaultValue: string
  placeholder: string
  items: Array<{ value: string; label: string }>
}) {
  return (
    <Select name={name} defaultValue={defaultValue}>
      <SelectTrigger className="h-10 w-full">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent align="start">
        <SelectGroup>
          <SelectItem value="">{placeholder}</SelectItem>
          {items.map((item) => (
            <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  )
}

function EntryRow({ entry, dict }: { entry: ReportsEntry; dict: Dictionary }) {
  const label = dict.dashboard.status[entry.status as keyof typeof dict.dashboard.status] ?? entry.status
  const variant = STATUS_VARIANT[entry.status] ?? 'secondary'
  const net = entry.received_value > 0 ? entry.received_value : entry.receivable_value

  return (
    <TableRow className="group">
      <TableCell className="pl-5 font-mono text-xs text-muted-foreground">{entry.order_id}</TableCell>
      <TableCell className="max-w-48">
        <p className="truncate font-medium">{entry.buyer_name}</p>
      </TableCell>
      <TableCell>{entry.platform}</TableCell>
      <TableCell className="text-xs text-muted-foreground">{formatDate(entry.sale_date)}</TableCell>
      <TableCell className="text-xs text-muted-foreground">
        {dict.sales.paymentMethods[entry.payment_method as keyof typeof dict.sales.paymentMethods] ?? entry.payment_method}
      </TableCell>
      <TableCell className="text-right font-medium">{formatCurrency(entry.gross_value)}</TableCell>
      <TableCell className="text-right text-muted-foreground">− {formatCurrency(entry.fee_value)}</TableCell>
      <TableCell className="text-right font-semibold">{formatCurrency(net)}</TableCell>
      <TableCell className="text-xs text-muted-foreground">{formatDate(entry.release_date)}</TableCell>
      <TableCell className="pr-5"><Badge variant={variant}>{label}</Badge></TableCell>
    </TableRow>
  )
}

function EmptyState({
  hasFilters,
  lang,
  dict,
}: {
  hasFilters: boolean
  lang: Locale
  dict: Dictionary
}) {
  return (
    <div className="flex min-h-72 flex-col items-center justify-center gap-5 px-6 py-16 text-center">
      <div className="flex size-12 items-center justify-center rounded-full border border-border bg-muted/40">
        {hasFilters ? <Search className="size-5 text-muted-foreground" /> : <FileText className="size-5 text-muted-foreground" />}
      </div>
      <div className="max-w-sm">
        <p className="font-medium">{dict.sales.table.empty.title}</p>
        <p className="mt-1 text-sm leading-6 text-muted-foreground">
          {hasFilters
            ? 'Nenhum pedido corresponde aos filtros escolhidos. Tente ampliar o período ou remover um filtro.'
            : dict.sales.table.empty.hint}
        </p>
      </div>
      {hasFilters ? (
        <Button variant="outline" render={<Link href={`/${lang}/lancamentos`} />}>
          <X />
          Limpar filtros
        </Button>
      ) : (
        <Button variant="outline" render={<Link href={`/${lang}/conectores`} />}>
          <CircleDollarSign />
          Ver conectores
        </Button>
      )}
    </div>
  )
}

function PaginationLink({
  href,
  disabled,
  label,
  icon,
}: {
  href: string
  disabled: boolean
  label: string
  icon: 'previous' | 'next'
}) {
  return (
    <Link
      href={href}
      aria-disabled={disabled}
      className={cnPagination(disabled)}
    >
      {icon === 'previous' ? <ArrowLeft className="size-4" /> : null}
      <span className="hidden sm:inline">{label}</span>
      {icon === 'next' ? <ArrowRight className="size-4" /> : null}
    </Link>
  )
}

function cnPagination(disabled: boolean) {
  return disabled
    ? 'pointer-events-none inline-flex h-9 items-center gap-2 rounded-md border border-border bg-muted/30 px-3 text-sm text-muted-foreground opacity-50'
    : 'inline-flex h-9 items-center gap-2 rounded-md border border-input bg-background px-3 text-sm font-medium transition hover:border-foreground/30 hover:bg-muted'
}

function getActiveFilters(
  sp: Awaited<Props['searchParams']>,
  dict: Dictionary
) {
  const filters: Array<{ key: string; label: string }> = []
  if (sp.search) filters.push({ key: 'search', label: `Busca: ${sp.search}` })
  if (sp.from) filters.push({ key: 'from', label: `A partir de ${formatDate(sp.from)}` })
  if (sp.to) filters.push({ key: 'to', label: `Até ${formatDate(sp.to)}` })
  if (sp.platform) filters.push({ key: 'platform', label: sp.platform })
  if (sp.status) {
    filters.push({
      key: 'status',
      label: dict.dashboard.status[sp.status as keyof typeof dict.dashboard.status] ?? sp.status,
    })
  }
  if (sp.paymentMethod) {
    filters.push({
      key: 'paymentMethod',
      label: dict.sales.paymentMethods[sp.paymentMethod as keyof typeof dict.sales.paymentMethods] ?? sp.paymentMethod,
    })
  }
  return filters
}
