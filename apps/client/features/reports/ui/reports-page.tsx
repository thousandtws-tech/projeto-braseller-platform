import { ArrowDownToLine, Download, FileSignature, TrendingDown, TrendingUp, Wallet } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { getToken, getSession } from '@/entities/session/server/session'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import {
  getAccountingClosing,
  getFiscalProfile,
  getProfitAvailability,
  getProfitDistributions,
  getReportsDre,
  getReportsEntries,
  formatCurrency,
} from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import type { FiscalProfile, ProfitAvailability, ProfitDistribution, ReportsDre, ReportsEntry } from '@/shared/types'
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
  const date = new Date(parseInt(year), parseInt(month) - 1, 1)
  return date.toLocaleDateString(LOCALE_MAP[lang], { month: 'long', year: 'numeric' })
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
  const dict = await getDictionary(lang)
  const token = (await getToken()) ?? ''
  const session = await getSession()
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

  // Prefer API DRE (includes CMV + banking expenses) over client-side calculation
  const dreData: ReportsDre = dreFromApi
    ? dreFromApi
    : buildDreFromEntries(tenantId, from, to, fiscalProfile, entries.items)

  const profitMargin = dreData.gross_revenue > 0
    ? (dreData.net_result / dreData.gross_revenue) * 100
    : 0
  const totalDeductions =
    dreData.marketplace_fees + dreData.estimated_taxes + (dreData.cmv ?? 0) +
    dreData.operating_expenses + (dreData.banking_expenses ?? 0)
  const profitBalance = resolveProfitBalance(profitAvailability, CURRENT_PERIOD, closing?.distributable_profit ?? dreData.distributable_profit ?? 0)
  const availableProfit = profitBalance.available_profit

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-semibold">{dict.dre.title}</h2>
          <p className="text-sm text-muted-foreground capitalize">
            {dict.dre.subtitle} - {periodLabel}
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          {dreData.tax_regime && (
            <Badge variant="secondary" className="h-8 px-3">
              {dict.dre.taxRegimes[dreData.tax_regime as keyof Dictionary['dre']['taxRegimes']] ?? dreData.tax_regime}
            </Badge>
          )}
          <Badge variant="secondary" className="h-8 px-3">
            {dict.dre.effectiveRate} {formatTaxRate(dreData.estimated_tax_rate, lang)}
          </Badge>
          {closing ? (
            <Badge variant="success" className="h-8 px-3">
              <FileSignature className="size-3 mr-1" /> {dict.dre.signed}
            </Badge>
          ) : (
            <Badge variant="outline" className="h-8 px-3">{dict.dre.open}</Badge>
          )}
          <Button variant="outline" size="sm">
            <Download className="size-3.5" />
            {dict.common.exportPdf}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader className="flex-row items-center justify-between pb-3">
            <CardTitle>{dict.dre.statementTitle}</CardTitle>
            <span className="text-xs text-muted-foreground">{dreData.sales_count} {dict.dre.salesCount}</span>
          </CardHeader>
          <CardContent>
            <div className="space-y-0 divide-y divide-border">
              <DreRow label={dict.dre.rows.grossRevenue} value={dreData.gross_revenue} type="income" />
              <DreRow label={dict.dre.rows.marketplaceFees} value={dreData.marketplace_fees} type="deduction" />
              <DreRow label={dict.dre.rows.estimatedTaxes} value={dreData.estimated_taxes} type="deduction" />
              {(dreData.cmv ?? 0) > 0 && (
                <DreRow label={dict.dre.rows.cmv} value={dreData.cmv ?? 0} type="deduction" />
              )}
              <DreRow label={dict.dre.rows.operatingExpenses} value={dreData.operating_expenses} type="deduction" />
              {(dreData.banking_expenses ?? 0) > 0 && (
                <DreRow label={dict.dre.rows.bankingExpenses} value={dreData.banking_expenses ?? 0} type="deduction" />
              )}
              <DreRow label={dict.dre.rows.netResult} value={dreData.net_result} type="total" />
            </div>
            <div className="mt-6 pt-4 border-t-2 border-foreground flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">{dict.dre.netMargin}</p>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className={`text-3xl font-bold ${profitMargin >= 0 ? 'text-emerald-600' : 'text-destructive'}`}>
                    {profitMargin.toFixed(1)}%
                  </span>
                  {profitMargin >= 0
                    ? <TrendingUp className="size-5 text-emerald-600" />
                    : <TrendingDown className="size-5 text-destructive" />
                  }
                </div>
              </div>
              <div className="text-right">
                <p className="text-xs text-muted-foreground">{dict.dre.netResultLabel}</p>
                <p className={`text-2xl font-bold mt-0.5 ${dreData.net_result >= 0 ? 'text-emerald-600' : 'text-destructive'}`}>
                  {formatCurrency(dreData.net_result)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader><CardTitle>{dict.dre.summary.title}</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {[
                { label: dict.dre.summary.grossRevenue, value: dreData.gross_revenue, color: 'text-primary', icon: TrendingUp },
                { label: dict.dre.summary.totalDeductions, value: totalDeductions, color: 'text-destructive', icon: TrendingDown },
                {
                  label: dict.dre.summary.result,
                  value: dreData.net_result,
                  color: dreData.net_result >= 0 ? 'text-emerald-600' : 'text-destructive',
                  icon: dreData.net_result >= 0 ? TrendingUp : TrendingDown,
                },
              ].map((item) => (
                <div key={item.label} className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
                  <div className="flex items-center gap-2">
                    <item.icon className={`size-4 ${item.color}`} />
                    <span className="text-sm text-muted-foreground">{item.label}</span>
                  </div>
                  <span className={`text-sm font-semibold ${item.color}`}>
                    {formatCurrency(item.value)}
                  </span>
                </div>
              ))}
            </CardContent>
          </Card>

          {dreData.expenses_by_category.length > 0 && (
            <Card>
              <CardHeader><CardTitle>{dict.dre.expensesByCategory}</CardTitle></CardHeader>
              <CardContent className="space-y-3">
                {dreData.expenses_by_category.map((cat) => (
                  <div key={cat.category} className="space-y-1">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">
                        {dict.dre.categories[cat.category as keyof Dictionary['dre']['categories']] ?? cat.category}
                      </span>
                      <span className="font-medium">{formatCurrency(cat.amount)}</span>
                    </div>
                    <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                      <div
                        className="h-full bg-destructive/50 rounded-full"
                        style={{ width: `${dreData.operating_expenses > 0 ? (cat.amount / dreData.operating_expenses) * 100 : 0}%` }}
                      />
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}

          {closing && profitBalance.distributable_profit > 0 && (
            <Card className="border-emerald-500/40 bg-emerald-500/5">
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2">
                  <ArrowDownToLine className="size-4 text-emerald-600" />
                  {dict.dre.profitAvailable.title}
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <p className="text-xs text-muted-foreground">{dict.dre.profitAvailable.availableBalance}</p>
                  <p className="text-3xl font-bold text-emerald-600">
                    {formatCurrency(availableProfit)}
                  </p>
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="rounded-lg border border-border bg-background/70 p-2">
                    <p className="text-muted-foreground">{dict.dre.profitAvailable.released}</p>
                    <p className="font-semibold">{formatCurrency(profitBalance.distributable_profit)}</p>
                  </div>
                  <div className="rounded-lg border border-border bg-background/70 p-2">
                    <p className="text-muted-foreground">{dict.dre.profitAvailable.distributed}</p>
                    <p className="font-semibold">{formatCurrency(profitBalance.distributed_profit)}</p>
                  </div>
                </div>
                <ProfitDistributionForm
                  periodMonth={CURRENT_PERIOD}
                  availableProfit={availableProfit}
                  disabled={availableProfit <= 0 || readOnly}
                  readOnly={readOnly}
                />
                {profitDistributions.length > 0 && (
                  <div className="space-y-2 pt-2 border-t border-emerald-500/20">
                    <p className="text-xs font-medium text-muted-foreground">{dict.dre.profitAvailable.historyTitle}</p>
                    <div className="space-y-2">
                      {profitDistributions.slice(0, 4).map((distribution) => (
                        <div key={distribution.id} className="flex items-center justify-between gap-3 rounded-lg bg-background/70 px-3 py-2 text-xs">
                          <div className="min-w-0">
                            <p className="font-medium truncate">
                              {distribution.recipient_name || dict.dre.profitAvailable.defaultRecipient}
                            </p>
                            <p className="text-muted-foreground">
                              {new Date(distribution.distributed_at).toLocaleDateString(LOCALE_MAP[lang])}
                            </p>
                          </div>
                          <span className="font-semibold text-emerald-700 dark:text-emerald-400">
                            {formatCurrency(distribution.amount)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {!closing && dreData.distributable_profit > 0 && (
            <Card>
              <CardContent className="pt-6 pb-5 flex flex-col items-center gap-2 text-center">
                <div className="size-10 rounded-full bg-muted flex items-center justify-center">
                  <Wallet className="size-5 text-muted-foreground" />
                </div>
                <p className="text-sm font-medium">{dict.dre.profitNotAvailable.title}</p>
                <p className="text-xs text-muted-foreground max-w-[220px]">
                  {formatMessage(dict.dre.profitNotAvailable.description, { amount: formatCurrency(dreData.distributable_profit) })}
                </p>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader><CardTitle>{dict.dre.signature.title}</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {closing ? (
                <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/5 p-4 space-y-1">
                  <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
                    {dict.dre.signature.closedPeriod}
                  </p>
                  <p className="text-xs text-muted-foreground">{formatMessage(dict.dre.signature.signedBy, { email: closing.signed_by_email })}</p>
                  <p className="text-xs text-muted-foreground">
                    {new Date(closing.signed_at).toLocaleString(LOCALE_MAP[lang])}
                  </p>
                </div>
              ) : (
                <div className="rounded-lg border border-dashed border-border p-4 text-center">
                  <FileSignature className="size-8 text-muted-foreground mx-auto mb-2" />
                  <p className="text-sm text-muted-foreground">{dict.dre.signature.openPeriod}</p>
                  <p className="text-xs text-muted-foreground/60 mt-1">
                    {dict.dre.signature.openPeriodHint}
                  </p>
                </div>
              )}
              {!closing && (
                <Button className="w-full" size="sm" disabled>
                  <FileSignature className="size-3.5" />
                  {dict.dre.signature.signButton}
                </Button>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
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

function formatTaxRate(rate: number, lang: Locale): string {
  const normalized = Number.isFinite(rate) ? rate : 0
  return new Intl.NumberFormat(LOCALE_MAP[lang], {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(normalized)
}

function DreRow({ label, value, type }: {
  label: string
  value: number
  type: 'income' | 'deduction' | 'total'
}) {
  const isDeduction = type === 'deduction'
  const isTotal = type === 'total'
  return (
    <div className={`flex items-center justify-between py-3 ${isTotal ? 'font-semibold' : ''}`}>
      <span className={`text-sm ${isTotal ? 'text-foreground font-semibold' : 'text-muted-foreground'}`}>
        {label}
      </span>
      <span className={`text-sm font-medium tabular-nums ${
        isTotal ? (value >= 0 ? 'text-emerald-600 text-base' : 'text-destructive text-base')
        : isDeduction ? 'text-destructive' : 'text-foreground'
      }`}>
        {isDeduction ? `-${formatCurrency(value)}` : formatCurrency(value)}
      </span>
    </div>
  )
}
