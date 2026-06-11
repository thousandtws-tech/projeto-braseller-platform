import { Landmark, Package, Receipt, ScrollText, Wallet } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { getToken, getSession } from '@/entities/session/server/session'
import { getReportsBalanceSheet, formatCurrency } from '@/shared/api/gateway'
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
  const dict = await getDictionary(lang)
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId ?? ''

  const today = new Date()
  const asOf = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  const data: ReportsBalanceSheet = tenantId
    ? (await getReportsBalanceSheet(token, tenantId, asOf)) ?? { ...EMPTY_BALANCE_SHEET, tenant_id: tenantId, as_of: asOf }
    : { ...EMPTY_BALANCE_SHEET, as_of: asOf }

  const asOfLabel = data.as_of
    ? new Date(`${data.as_of}T00:00:00`).toLocaleDateString(LOCALE_MAP[lang], { day: '2-digit', month: 'long', year: 'numeric' })
    : ''

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-semibold">{dict.balanceSheet.title}</h2>
          <p className="text-sm text-muted-foreground capitalize">
            {formatMessage(dict.balanceSheet.subtitle, { date: asOfLabel })}
          </p>
        </div>
        <Badge variant="secondary" className="h-8 px-3">
          {dict.balanceSheet.equity} {formatCurrency(data.equity)}
        </Badge>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card>
          <CardHeader className="flex-row items-center justify-between pb-3">
            <CardTitle>{dict.balanceSheet.assets.title}</CardTitle>
            <span className="text-xs text-muted-foreground">{dict.balanceSheet.assets.subtitle}</span>
          </CardHeader>
          <CardContent>
            <div className="space-y-0 divide-y divide-border">
              <BalanceRow icon={Landmark} label={dict.balanceSheet.assets.cashAndBank} value={data.cash_and_bank} />
              <BalanceRow icon={Receipt} label={dict.balanceSheet.assets.accountsReceivable} value={data.accounts_receivable} />
              <BalanceRow icon={Package} label={dict.balanceSheet.assets.inventory} value={data.inventory} />
            </div>
            <div className="mt-4 pt-4 border-t-2 border-foreground flex items-center justify-between">
              <span className="text-sm font-semibold">{dict.balanceSheet.assets.total}</span>
              <span className="text-2xl font-bold tabular-nums">{formatCurrency(data.total_assets)}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between pb-3">
            <CardTitle>{dict.balanceSheet.liabilities.title}</CardTitle>
            <span className="text-xs text-muted-foreground">{dict.balanceSheet.liabilities.subtitle}</span>
          </CardHeader>
          <CardContent>
            <div className="space-y-0 divide-y divide-border">
              <BalanceRow icon={ScrollText} label={dict.balanceSheet.liabilities.accountsPayable} value={data.accounts_payable} />
              <BalanceRow icon={Receipt} label={dict.balanceSheet.liabilities.taxesPayable} value={data.taxes_payable} />
              <BalanceRow icon={Wallet} label={dict.balanceSheet.equity} value={data.equity} />
            </div>
            <div className="mt-4 pt-4 border-t-2 border-foreground flex items-center justify-between">
              <span className="text-sm font-semibold">{dict.balanceSheet.liabilities.total}</span>
              <span className="text-2xl font-bold tabular-nums">{formatCurrency(data.total_liabilities_and_equity)}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader><CardTitle>{dict.balanceSheet.reconciliation.title}</CardTitle></CardHeader>
        <CardContent>
          <div className="flex items-center justify-between py-2">
            <span className="text-sm text-muted-foreground">
              {dict.balanceSheet.reconciliation.accumulatedResult}
            </span>
            <span className={`text-sm font-semibold tabular-nums ${data.accumulated_net_result >= 0 ? 'text-emerald-600' : 'text-destructive'}`}>
              {formatCurrency(data.accumulated_net_result)}
            </span>
          </div>
          <p className="text-xs text-muted-foreground mt-2">
            {dict.balanceSheet.reconciliation.note}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

function BalanceRow({ icon: Icon, label, value }: {
  icon: React.ComponentType<{ className?: string }>
  label: string
  value: number
}) {
  return (
    <div className="flex items-center justify-between py-3">
      <div className="flex items-center gap-2">
        <Icon className="size-4 text-muted-foreground" />
        <span className="text-sm text-muted-foreground">{label}</span>
      </div>
      <span className="text-sm font-medium tabular-nums">{formatCurrency(value)}</span>
    </div>
  )
}
