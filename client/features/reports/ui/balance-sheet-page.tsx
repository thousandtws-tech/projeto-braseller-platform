import { Landmark, Package, Receipt, Scale, ScrollText, Wallet } from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { getToken, getSession } from '@/entities/session/server/session'
import { formatCurrency, getReportsBalanceSheet } from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import type { ReportsBalanceSheet } from '@/shared/types'

const LOCALE_MAP: Record<Locale, string> = {
  'pt-BR': 'pt-BR',
  en: 'en-US',
  es: 'es-ES',
}

const EMPTY_BALANCE_SHEET: ReportsBalanceSheet = {
  tenant_id: '',
  as_of: '',
  cash_and_bank: 0,
  accounts_receivable: 0,
  inventory: 0,
  total_assets: 0,
  accounts_payable: 0,
  taxes_payable: 0,
  total_liabilities: 0,
  equity: 0,
  accumulated_net_result: 0,
  total_liabilities_and_equity: 0,
}

interface PageProps {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: PageProps) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.balanceSheet.title }
}

export default async function BalanceSheetPage({ params }: PageProps) {
  const { lang } = await params
  const [dict, token, session] = await Promise.all([
    getDictionary(lang),
    getToken().then((value) => value ?? ''),
    getSession(),
  ])
  const tenantId = session?.tenantId ?? ''
  const today = new Date()
  const asOf = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  const data = tenantId
    ? (await getReportsBalanceSheet(token, tenantId, asOf)) ?? { ...EMPTY_BALANCE_SHEET, tenant_id: tenantId, as_of: asOf }
    : { ...EMPTY_BALANCE_SHEET, as_of: asOf }
  const asOfLabel = new Date(`${data.as_of || asOf}T00:00:00`).toLocaleDateString(LOCALE_MAP[lang], {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
  const reconciliationDifference = data.total_assets - data.total_liabilities_and_equity
  const reconciled = Math.abs(reconciliationDifference) < 0.01

  return (
    <div className="flex w-full flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.balanceSheet.title}</h2>
          <p className="mt-1 text-sm capitalize text-muted-foreground">
            {formatMessage(dict.balanceSheet.subtitle, { date: asOfLabel })}
          </p>
        </div>
        <Badge variant={reconciled ? 'secondary' : 'warning'} className="h-8 px-3">
          {reconciled ? 'Balanço conciliado' : 'Revisão necessária'}
        </Badge>
      </header>

      <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
        <Metric label="Ativo total" value={data.total_assets} helper="Bens e direitos" icon={Landmark} />
        <Metric label="Passivo total" value={data.total_liabilities} helper="Obrigações" icon={ScrollText} />
        <Metric label={dict.balanceSheet.equity} value={data.equity} helper="Capital próprio" icon={Wallet} />
        <Metric label="Diferença contábil" value={reconciliationDifference} helper={reconciled ? 'Equação equilibrada' : 'Ativo menos passivo + PL'} icon={Scale} />
      </section>

      <section className="grid gap-6 xl:grid-cols-2">
        <StatementPanel
          title={dict.balanceSheet.assets.title}
          description={dict.balanceSheet.assets.subtitle}
          totalLabel={dict.balanceSheet.assets.total}
          total={data.total_assets}
          rows={[
            { icon: Landmark, label: dict.balanceSheet.assets.cashAndBank, value: data.cash_and_bank },
            { icon: Receipt, label: dict.balanceSheet.assets.accountsReceivable, value: data.accounts_receivable },
            { icon: Package, label: dict.balanceSheet.assets.inventory, value: data.inventory },
          ]}
        />
        <StatementPanel
          title={dict.balanceSheet.liabilities.title}
          description={dict.balanceSheet.liabilities.subtitle}
          totalLabel={dict.balanceSheet.liabilities.total}
          total={data.total_liabilities_and_equity}
          rows={[
            { icon: ScrollText, label: dict.balanceSheet.liabilities.accountsPayable, value: data.accounts_payable },
            { icon: Receipt, label: dict.balanceSheet.liabilities.taxesPayable, value: data.taxes_payable },
            { icon: Wallet, label: dict.balanceSheet.equity, value: data.equity },
          ]}
        />
      </section>

      <Card>
        <CardHeader className="flex-row items-center justify-between gap-4">
          <div>
            <CardTitle>{dict.balanceSheet.reconciliation.title}</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">Validação cruzada entre balanço e resultado acumulado.</p>
          </div>
          <Badge variant={reconciled ? 'secondary' : 'destructive'}>
            {reconciled ? 'Sem divergências' : formatCurrency(reconciliationDifference)}
          </Badge>
        </CardHeader>
        <CardContent className="grid gap-5 md:grid-cols-[1fr_auto] md:items-center">
          <div>
            <p className="text-sm font-medium">{dict.balanceSheet.reconciliation.accumulatedResult}</p>
            <p className="mt-1 text-sm leading-6 text-muted-foreground">{dict.balanceSheet.reconciliation.note}</p>
          </div>
          <p className={data.accumulated_net_result < 0 ? 'text-2xl font-semibold tabular-nums text-destructive' : 'text-2xl font-semibold tabular-nums'}>
            {formatCurrency(data.accumulated_net_result)}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

function Metric({ label, value, helper, icon: Icon }: { label: string; value: number; helper: string; icon: React.ComponentType<{ className?: string }> }) {
  return (
    <div className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0">
      <div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div>
      <p className={value < 0 ? 'text-2xl font-semibold tracking-[-0.035em] tabular-nums text-destructive' : 'text-2xl font-semibold tracking-[-0.035em] tabular-nums'}>{formatCurrency(value)}</p>
      <p className="text-[11px] text-muted-foreground">{helper}</p>
    </div>
  )
}

function StatementPanel({
  title,
  description,
  totalLabel,
  total,
  rows,
}: {
  title: string
  description: string
  totalLabel: string
  total: number
  rows: Array<{ icon: React.ComponentType<{ className?: string }>; label: string; value: number }>
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <p className="text-xs text-muted-foreground">{description}</p>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y divide-border">
          {rows.map((row) => (
            <div key={row.label} className="flex items-center justify-between gap-4 px-5 py-4">
              <div className="flex min-w-0 items-center gap-3">
                <row.icon className="size-4 shrink-0 text-muted-foreground" />
                <span className="truncate text-sm text-muted-foreground">{row.label}</span>
              </div>
              <span className="text-sm font-medium tabular-nums">{formatCurrency(row.value)}</span>
            </div>
          ))}
        </div>
        <div className="flex items-center justify-between gap-4 border-t-2 border-foreground px-5 py-5">
          <span className="text-sm font-semibold">{totalLabel}</span>
          <span className="text-2xl font-semibold tracking-[-0.035em] tabular-nums">{formatCurrency(total)}</span>
        </div>
      </CardContent>
    </Card>
  )
}
