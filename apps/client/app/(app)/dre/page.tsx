import type { Metadata } from 'next'
import { Download, FileSignature, TrendingDown, TrendingUp } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { getToken, getSession } from '@/lib/auth'
import { getAccountingClosing, getFiscalProfile, getReportsEntries, formatCurrency } from '@/lib/api'
import type { FiscalProfile, ReportsDre, ReportsEntry } from '@/types'

export const metadata: Metadata = { title: 'DRE' }

const CATEGORY_LABELS: Record<string, string> = {
  OPERATIONAL: 'Operacional',
  PACKAGING: 'Embalagens',
  SUPPLIES: 'Suprimentos',
  LABOR: 'Mão de obra',
  BANK_FEE: 'Tarifas bancárias',
  SHIPPING: 'Frete',
  TAX: 'Impostos',
  OTHER: 'Outros',
}

const TAX_REGIME_LABELS: Record<string, string> = {
  SIMPLES_NACIONAL: 'Simples Nacional',
  LUCRO_PRESUMIDO: 'Lucro Presumido',
  LUCRO_REAL: 'Lucro Real',
  MEI: 'MEI',
}

const CURRENT_PERIOD = (() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})()

const PERIOD_LABEL = (() => {
  const [year, month] = CURRENT_PERIOD.split('-')
  const date = new Date(parseInt(year), parseInt(month) - 1, 1)
  return date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
})()

export default async function DrePage() {
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId ?? ''

  const now = new Date()
  const from = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  const to = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${lastDay}`

  const [entries, fiscalProfile, closing] = await Promise.all([
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
  ])

  const dreData = buildDreFromEntries(tenantId, from, to, fiscalProfile, entries.items)
  const profitMargin = dreData.gross_revenue > 0
    ? (dreData.net_result / dreData.gross_revenue) * 100
    : 0
  const totalDeductions =
    dreData.marketplace_fees + dreData.estimated_taxes + dreData.operating_expenses

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-semibold">DRE</h2>
          <p className="text-sm text-muted-foreground capitalize">
            Demonstracao do Resultado - {PERIOD_LABEL}
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          {dreData.tax_regime && (
            <Badge variant="secondary" className="h-8 px-3">
              {TAX_REGIME_LABELS[dreData.tax_regime] ?? dreData.tax_regime}
            </Badge>
          )}
          <Badge variant="secondary" className="h-8 px-3">
            Aliquota {dreData.estimated_tax_rate}%
          </Badge>
          {closing ? (
            <Badge variant="success" className="h-8 px-3">
              <FileSignature className="size-3 mr-1" /> Assinado
            </Badge>
          ) : (
            <Badge variant="outline" className="h-8 px-3">Aberto</Badge>
          )}
          <Button variant="outline" size="sm">
            <Download className="size-3.5" />
            Exportar PDF
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader className="flex-row items-center justify-between pb-3">
            <CardTitle>Demonstracao do Resultado</CardTitle>
            <span className="text-xs text-muted-foreground">{dreData.sales_count} vendas</span>
          </CardHeader>
          <CardContent>
            <div className="space-y-0 divide-y divide-border">
              <DreRow label="Receita Bruta de Vendas" value={dreData.gross_revenue} type="income" />
              <DreRow label="(-) Taxas de Marketplace" value={dreData.marketplace_fees} type="deduction" />
              <DreRow label="(-) Impostos Estimados" value={dreData.estimated_taxes} type="deduction" />
              <DreRow label="(-) Despesas Operacionais" value={dreData.operating_expenses} type="deduction" />
              <DreRow label="Resultado Liquido" value={dreData.net_result} type="total" />
            </div>
            <div className="mt-6 pt-4 border-t-2 border-foreground flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">Margem Liquida</p>
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
                <p className="text-xs text-muted-foreground">Resultado Liquido</p>
                <p className={`text-2xl font-bold mt-0.5 ${dreData.net_result >= 0 ? 'text-emerald-600' : 'text-destructive'}`}>
                  {formatCurrency(dreData.net_result)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader><CardTitle>Resumo Financeiro</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {[
                { label: 'Receita Bruta', value: dreData.gross_revenue, color: 'text-primary', icon: TrendingUp },
                { label: 'Total Deducoes', value: totalDeductions, color: 'text-destructive', icon: TrendingDown },
                {
                  label: 'Resultado',
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
              <CardHeader><CardTitle>Despesas por Categoria</CardTitle></CardHeader>
              <CardContent className="space-y-3">
                {dreData.expenses_by_category.map((cat) => (
                  <div key={cat.category} className="space-y-1">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">
                        {CATEGORY_LABELS[cat.category] ?? cat.category}
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

          <Card>
            <CardHeader><CardTitle>Assinatura Digital</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {closing ? (
                <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/5 p-4 space-y-1">
                  <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
                    Periodo fechado
                  </p>
                  <p className="text-xs text-muted-foreground">Por {closing.signed_by_email}</p>
                  <p className="text-xs text-muted-foreground">
                    {new Date(closing.signed_at).toLocaleString('pt-BR')}
                  </p>
                </div>
              ) : (
                <div className="rounded-lg border border-dashed border-border p-4 text-center">
                  <FileSignature className="size-8 text-muted-foreground mx-auto mb-2" />
                  <p className="text-sm text-muted-foreground">Periodo ainda nao fechado</p>
                  <p className="text-xs text-muted-foreground/60 mt-1">
                    Assine para travar os dados do mes
                  </p>
                </div>
              )}
              {!closing && (
                <Button className="w-full" size="sm" disabled>
                  <FileSignature className="size-3.5" />
                  Assinar via Clicksign
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
  const estimatedTaxes = grossRevenue * (estimatedTaxRate / 100)
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
    operating_expenses: 0,
    net_result: netResult,
    sales_count: entries.length,
    expense_count: 0,
    expenses_by_category: [],
  }
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
