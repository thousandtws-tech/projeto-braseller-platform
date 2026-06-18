import {
  ArrowDownToLine,
  FileSignature,
  Percent,
  ReceiptText,
  TrendingDown,
  TrendingUp,
  Wallet,
} from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { getToken, getSession } from '@/entities/session/server/session'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import {
  formatCurrency,
  getAccountingClosing,
  getFiscalProfile,
  getProfitAvailability,
  getProfitDistributions,
  getReportsDre,
  getReportsEntries,
} from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import type {
  FiscalProfile,
  ProfitAvailability,
  ProfitDistribution,
  ReportsDre,
  ReportsEntry,
} from '@/shared/types'
import { ProfitDistributionForm } from './profit-distribution-form'

const LOCALE_MAP: Record<Locale, string> = {
  'pt-BR': 'pt-BR',
  en: 'en-US',
  es: 'es-ES',
}

const CURRENT_PERIOD = (() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})()

function getPeriodLabel(lang: Locale) {
  const [year, month] = CURRENT_PERIOD.split('-')
  return new Date(Number(year), Number(month) - 1, 1).toLocaleDateString(LOCALE_MAP[lang], {
    month: 'long',
    year: 'numeric',
  })
}

interface PageProps {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: PageProps) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.dre.title }
}

export default async function DrePage({ params }: PageProps) {
  const { lang } = await params
  const [dict, token, session] = await Promise.all([
    getDictionary(lang),
    getToken().then((value) => value ?? ''),
    getSession(),
  ])
  const tenantId = session?.tenantId ?? ''
  const readOnly = isReadOnlyAccountant(session?.roles)
  const periodLabel = getPeriodLabel(lang)
  const now = new Date()
  const from = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  const to = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${lastDay}`

  const [entries, fiscalProfile, closing, dreFromApi, profitAvailability, profitDistributions] = await Promise.all([
    tenantId
      ? getReportsEntries(token, tenantId, { from, to, page: 0, size: 1000 }).catch(() => ({
          items: [] as ReportsEntry[],
          total: 0,
          page: 0,
          size: 1000,
        }))
      : Promise.resolve({ items: [] as ReportsEntry[], total: 0, page: 0, size: 1000 }),
    tenantId ? getFiscalProfile(token, tenantId) : Promise.resolve(null),
    tenantId ? getAccountingClosing(token, tenantId, CURRENT_PERIOD) : Promise.resolve(null),
    tenantId ? getReportsDre(token, tenantId, from, to).catch(() => null) : Promise.resolve(null),
    tenantId ? getProfitAvailability(token, tenantId) : Promise.resolve(null),
    tenantId ? getProfitDistributions(token, tenantId, CURRENT_PERIOD) : Promise.resolve([] as ProfitDistribution[]),
  ])

  const dreData = dreFromApi ?? buildDreFromEntries(tenantId, from, to, fiscalProfile, entries.items)
  const profitMargin = dreData.gross_revenue > 0 ? (dreData.net_result / dreData.gross_revenue) * 100 : 0
  const totalDeductions =
    dreData.marketplace_fees +
    dreData.estimated_taxes +
    (dreData.cmv ?? 0) +
    dreData.operating_expenses +
    (dreData.banking_expenses ?? 0)
  const profitBalance = resolveProfitBalance(
    profitAvailability,
    CURRENT_PERIOD,
    closing?.distributable_profit ?? dreData.distributable_profit ?? 0
  )
  const statementRows = [
    { label: dict.dre.rows.grossRevenue, value: dreData.gross_revenue, kind: 'income' as const },
    { label: dict.dre.rows.marketplaceFees, value: dreData.marketplace_fees, kind: 'deduction' as const },
    { label: dict.dre.rows.estimatedTaxes, value: dreData.estimated_taxes, kind: 'deduction' as const },
    ...(dreData.cmv ?? 0) > 0
      ? [{ label: dict.dre.rows.cmv, value: dreData.cmv ?? 0, kind: 'deduction' as const }]
      : [],
    { label: dict.dre.rows.operatingExpenses, value: dreData.operating_expenses, kind: 'deduction' as const },
    ...(dreData.banking_expenses ?? 0) > 0
      ? [{ label: dict.dre.rows.bankingExpenses, value: dreData.banking_expenses ?? 0, kind: 'deduction' as const }]
      : [],
  ]

  return (
    <div className="flex w-full flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.dre.title}</h2>
          <p className="mt-1 text-sm capitalize text-muted-foreground">{dict.dre.subtitle} · {periodLabel}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {dreData.tax_regime ? (
            <Badge variant="secondary" className="h-8 px-3">
              {dict.dre.taxRegimes[dreData.tax_regime as keyof Dictionary['dre']['taxRegimes']] ?? dreData.tax_regime}
            </Badge>
          ) : null}
          <Badge variant={closing ? 'secondary' : 'outline'} className="h-8 px-3">
            {closing ? dict.dre.signed : dict.dre.open}
          </Badge>
        </div>
      </header>

      <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
        <Metric label={dict.dre.summary.grossRevenue} value={formatCurrency(dreData.gross_revenue)} helper={`${dreData.sales_count} ${dict.dre.salesCount}`} icon={ReceiptText} />
        <Metric label={dict.dre.summary.totalDeductions} value={formatCurrency(totalDeductions)} helper="Taxas, impostos e custos" icon={TrendingDown} />
        <Metric label={dict.dre.summary.result} value={formatCurrency(dreData.net_result)} helper={dreData.net_result >= 0 ? 'Resultado positivo' : 'Resultado negativo'} icon={dreData.net_result >= 0 ? TrendingUp : TrendingDown} negative={dreData.net_result < 0} />
        <Metric label={dict.dre.netMargin} value={`${profitMargin.toFixed(1)}%`} helper={`${dict.dre.effectiveRate} ${formatTaxRate(dreData.estimated_tax_rate, lang)}`} icon={Percent} negative={profitMargin < 0} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <Card className="overflow-hidden">
          <CardHeader className="flex-row items-center justify-between gap-4">
            <div>
              <CardTitle>{dict.dre.statementTitle}</CardTitle>
              <p className="mt-1 text-xs text-muted-foreground">Composição do resultado do período.</p>
            </div>
            <Badge variant="outline">{periodLabel}</Badge>
          </CardHeader>
          <CardContent className="p-0">
            <div className="divide-y divide-border">
              {statementRows.map((row) => <DreRow key={row.label} {...row} />)}
            </div>
            <div className="flex flex-wrap items-end justify-between gap-5 border-t-2 border-foreground px-5 py-6">
              <div>
                <p className="text-xs text-muted-foreground">{dict.dre.rows.netResult}</p>
                <p className={dreData.net_result < 0 ? 'mt-1 text-3xl font-semibold tracking-[-0.04em] tabular-nums text-destructive' : 'mt-1 text-3xl font-semibold tracking-[-0.04em] tabular-nums'}>
                  {formatCurrency(dreData.net_result)}
                </p>
              </div>
              <div className="text-right">
                <p className="text-xs text-muted-foreground">{dict.dre.netMargin}</p>
                <p className={profitMargin < 0 ? 'mt-1 text-2xl font-semibold tabular-nums text-destructive' : 'mt-1 text-2xl font-semibold tabular-nums'}>
                  {profitMargin.toFixed(1)}%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <aside className="flex flex-col gap-6">
          <Card>
            <CardHeader><CardTitle>{dict.dre.signature.title}</CardTitle></CardHeader>
            <CardContent className="flex flex-col gap-4">
              {closing ? (
                <div className="rounded-md border border-border bg-muted/40 p-4">
                  <div className="flex items-center gap-2"><FileSignature className="size-4" /><p className="text-sm font-medium">{dict.dre.signature.closedPeriod}</p></div>
                  <p className="mt-2 text-xs text-muted-foreground">{formatMessage(dict.dre.signature.signedBy, { email: closing.signed_by_email })}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{new Date(closing.signed_at).toLocaleString(LOCALE_MAP[lang])}</p>
                </div>
              ) : (
                <>
                  <div className="rounded-md border border-dashed border-border p-5 text-center">
                    <FileSignature className="mx-auto size-6 text-muted-foreground" />
                    <p className="mt-3 text-sm font-medium">{dict.dre.signature.openPeriod}</p>
                    <p className="mt-1 text-xs leading-5 text-muted-foreground">{dict.dre.signature.openPeriodHint}</p>
                  </div>
                  <Button disabled className="w-full"><FileSignature />{dict.dre.signature.signButton}</Button>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>{dict.dre.expensesByCategory}</CardTitle></CardHeader>
            <CardContent className="flex flex-col gap-4">
              {dreData.expenses_by_category.length === 0 ? (
                <p className="text-sm leading-6 text-muted-foreground">As categorias aparecerão após o registro de despesas no período.</p>
              ) : dreData.expenses_by_category.map((category) => (
                <div key={category.category} className="flex flex-col gap-2">
                  <div className="flex items-center justify-between gap-3 text-sm">
                    <span className="truncate text-muted-foreground">{dict.dre.categories[category.category as keyof Dictionary['dre']['categories']] ?? category.category}</span>
                    <span className="font-semibold tabular-nums">{formatCurrency(category.amount)}</span>
                  </div>
                  <div className="h-1 overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full bg-foreground/55" style={{ width: `${dreData.operating_expenses > 0 ? (category.amount / dreData.operating_expenses) * 100 : 0}%` }} />
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </aside>
      </section>

      {closing && profitBalance.distributable_profit > 0 ? (
        <Card>
          <CardHeader className="flex-row items-center justify-between gap-4">
            <div>
              <CardTitle className="flex items-center gap-2"><ArrowDownToLine className="size-4" />{dict.dre.profitAvailable.title}</CardTitle>
              <p className="mt-1 text-xs text-muted-foreground">Controle de valores liberados e retiradas registradas.</p>
            </div>
            <p className="text-2xl font-semibold tabular-nums">{formatCurrency(profitBalance.available_profit)}</p>
          </CardHeader>
          <CardContent className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
            <div className="grid content-start gap-4 sm:grid-cols-2">
              <SummaryValue label={dict.dre.profitAvailable.released} value={profitBalance.distributable_profit} />
              <SummaryValue label={dict.dre.profitAvailable.distributed} value={profitBalance.distributed_profit} />
              {profitDistributions.length > 0 ? (
                <div className="sm:col-span-2">
                  <p className="mb-2 text-xs font-medium text-muted-foreground">{dict.dre.profitAvailable.historyTitle}</p>
                  <div className="divide-y divide-border rounded-md border border-border">
                    {profitDistributions.slice(0, 4).map((distribution) => (
                      <div key={distribution.id} className="flex items-center justify-between gap-3 px-4 py-3 text-sm">
                        <div className="min-w-0"><p className="truncate font-medium">{distribution.recipient_name || dict.dre.profitAvailable.defaultRecipient}</p><p className="text-xs text-muted-foreground">{new Date(distribution.distributed_at).toLocaleDateString(LOCALE_MAP[lang])}</p></div>
                        <span className="font-semibold tabular-nums">{formatCurrency(distribution.amount)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
            <ProfitDistributionForm periodMonth={CURRENT_PERIOD} availableProfit={profitBalance.available_profit} disabled={profitBalance.available_profit <= 0 || readOnly} readOnly={readOnly} />
          </CardContent>
        </Card>
      ) : null}

      {!closing && dreData.distributable_profit > 0 ? (
        <Card>
          <CardContent className="flex items-center gap-4 p-5">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-muted"><Wallet className="size-5 text-muted-foreground" /></div>
            <div><p className="text-sm font-medium">{dict.dre.profitNotAvailable.title}</p><p className="mt-1 text-xs text-muted-foreground">{formatMessage(dict.dre.profitNotAvailable.description, { amount: formatCurrency(dreData.distributable_profit) })}</p></div>
          </CardContent>
        </Card>
      ) : null}
    </div>
  )
}

function Metric({ label, value, helper, icon: Icon, negative = false }: { label: string; value: string; helper: string; icon: React.ComponentType<{ className?: string }>; negative?: boolean }) {
  return (
    <div className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0">
      <div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div>
      <p className={negative ? 'text-2xl font-semibold tracking-[-0.035em] tabular-nums text-destructive' : 'text-2xl font-semibold tracking-[-0.035em] tabular-nums'}>{value}</p>
      <p className="text-[11px] text-muted-foreground">{helper}</p>
    </div>
  )
}

function DreRow({ label, value, kind }: { label: string; value: number; kind: 'income' | 'deduction' }) {
  return (
    <div className="flex items-center justify-between gap-4 px-5 py-4">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className={kind === 'deduction' ? 'text-sm font-medium tabular-nums text-destructive' : 'text-sm font-medium tabular-nums'}>
        {kind === 'deduction' ? `− ${formatCurrency(value)}` : formatCurrency(value)}
      </span>
    </div>
  )
}

function SummaryValue({ label, value }: { label: string; value: number }) {
  return <div className="rounded-md border border-border bg-muted/35 p-4"><p className="text-xs text-muted-foreground">{label}</p><p className="mt-1 text-xl font-semibold tabular-nums">{formatCurrency(value)}</p></div>
}

function buildDreFromEntries(
  tenantId: string,
  from: string,
  to: string,
  fiscalProfile: FiscalProfile | null,
  entries: ReportsEntry[]
): ReportsDre {
  const grossRevenue = entries.reduce((sum, entry) => sum + entry.gross_value, 0)
  const marketplaceFees = entries.reduce((sum, entry) => sum + entry.fee_value, 0)
  const estimatedTaxRate = fiscalProfile?.estimated_tax_rate ?? 0
  const estimatedTaxes = grossRevenue * estimatedTaxRate
  const netResult = grossRevenue - marketplaceFees - estimatedTaxes
  return {
    tenant_id: tenantId,
    from,
    to,
    tax_regime: fiscalProfile?.tax_regime ?? '',
    estimated_tax_rate: estimatedTaxRate,
    gross_revenue: grossRevenue,
    marketplace_fees: marketplaceFees,
    estimated_taxes: estimatedTaxes,
    cmv: 0,
    operating_expenses: 0,
    banking_expenses: 0,
    net_result: netResult,
    distributable_profit: netResult > 0 ? netResult : 0,
    sales_count: entries.length,
    expense_count: 0,
    expenses_by_category: [],
  }
}

function resolveProfitBalance(
  availability: ProfitAvailability | null,
  periodMonth: string,
  fallbackDistributableProfit: number
) {
  const period = availability?.periods.find((item) => item.period_month === periodMonth)
  if (period) return period
  const distributableProfit = Math.max(0, fallbackDistributableProfit)
  return {
    period_month: periodMonth,
    signed_at: '',
    distributable_profit: distributableProfit,
    distributed_profit: 0,
    available_profit: distributableProfit,
  }
}

function formatTaxRate(rate: number, lang: Locale) {
  return new Intl.NumberFormat(LOCALE_MAP[lang], {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(rate) ? rate : 0)
}
