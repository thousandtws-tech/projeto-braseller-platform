import { TrendingDown } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { getToken, getSession } from '@/entities/session/server/session'
import { getBankTransactions, formatCurrency } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import { UploadOfxForm } from './upload-ofx-form'
import type { BankTransaction } from '@/shared/types'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

const CATEGORY_COLORS: Record<string, string> = {
  TARIFA_BANCARIA: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  JUROS: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  PIX: 'bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-400',
  TED_DOC: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  IOF: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
  OUTROS: 'bg-muted text-muted-foreground',
}

function currentMonthBounds() {
  const now = new Date()
  return {
    from: new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0],
    to: new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0],
  }
}

function summarizeByCategory(transactions: BankTransaction[]) {
  const debits = transactions.filter((t) => t.tran_type === 'DEBIT')
  const byCategory = debits.reduce<Record<string, number>>((acc, t) => {
    acc[t.category] = (acc[t.category] ?? 0) + t.amount
    return acc
  }, {})
  return Object.entries(byCategory).sort((a, b) => b[1] - a[1])
}

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.bank.title }
}

export default async function ExtratoPage({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const categoryLabels = dict.bank.categories as Record<string, string>
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId ?? ''
  const readOnly = isReadOnlyAccountant(session?.roles)
  const { from, to } = currentMonthBounds()

  const transactions = tenantId
    ? await getBankTransactions(token, tenantId, from, to)
    : []

  const debits = transactions.filter((t) => t.tran_type === 'DEBIT')
  const totalExpenses = debits.reduce((sum, t) => sum + t.amount, 0)
  const categorySummary = summarizeByCategory(transactions)

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h2 className="text-xl font-semibold">{dict.bank.header.title}</h2>
        <p className="text-sm text-muted-foreground">
          {dict.bank.header.subtitle}
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload + KPIs */}
        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardHeader><CardTitle>{dict.bank.importOfx.title}</CardTitle></CardHeader>
            <CardContent>
              <UploadOfxForm readOnly={readOnly} dict={dict} />
              <p className="mt-3 text-xs text-muted-foreground">
                {dict.bank.importOfx.hint}
              </p>
            </CardContent>
          </Card>

          {/* Transactions table */}
          <Card>
            <CardHeader className="flex-row items-center justify-between pb-3">
              <CardTitle>{dict.bank.transactions.title}</CardTitle>
              <span className="text-xs text-muted-foreground">{formatMessage(dict.bank.transactions.count, { count: transactions.length })}</span>
            </CardHeader>
            <CardContent>
              {transactions.length === 0 ? (
                <div className="py-8 text-center text-sm text-muted-foreground">
                  {dict.bank.transactions.empty}
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border text-muted-foreground">
                        <th className="text-left py-2 pr-4 font-medium">{dict.bank.transactions.columns.date}</th>
                        <th className="text-left py-2 pr-4 font-medium">{dict.bank.transactions.columns.description}</th>
                        <th className="text-left py-2 pr-4 font-medium">{dict.bank.transactions.columns.category}</th>
                        <th className="text-right py-2 font-medium">{dict.bank.transactions.columns.value}</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {transactions.map((t) => (
                        <tr key={t.id} className="hover:bg-muted/30 transition-colors">
                          <td className="py-2.5 pr-4 text-muted-foreground tabular-nums whitespace-nowrap">
                            {new Date(t.posted_date).toLocaleDateString(LOCALE_MAP[lang])}
                          </td>
                          <td className="py-2.5 pr-4 text-muted-foreground max-w-[200px] truncate">
                            {t.description ?? '—'}
                          </td>
                          <td className="py-2.5 pr-4">
                            <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${CATEGORY_COLORS[t.category] ?? CATEGORY_COLORS.OUTROS}`}>
                              {categoryLabels[t.category] ?? t.category}
                            </span>
                          </td>
                          <td className={`py-2.5 text-right font-medium tabular-nums ${t.tran_type === 'DEBIT' ? 'text-destructive' : 'text-emerald-600'}`}>
                            {t.tran_type === 'DEBIT' ? '-' : '+'}{formatCurrency(t.amount)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Summary sidebar */}
        <div className="space-y-4">
          <Card>
            <CardContent className="pt-6 space-y-2">
              <div className="flex items-center gap-3">
                <div className="size-10 rounded-full bg-destructive/10 flex items-center justify-center">
                  <TrendingDown className="size-5 text-destructive" />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{dict.bank.sidebar.totalExpenses}</p>
                  <p className="text-xl font-bold text-destructive">{formatCurrency(totalExpenses)}</p>
                </div>
              </div>
              <p className="text-xs text-muted-foreground border-t border-border pt-2">
                {dict.bank.sidebar.notePrefix} <strong>{dict.bank.sidebar.noteHighlight}</strong> {dict.bank.sidebar.noteSuffix}
              </p>
            </CardContent>
          </Card>

          {categorySummary.length > 0 && (
            <Card>
              <CardHeader><CardTitle>{dict.bank.sidebar.byCategory}</CardTitle></CardHeader>
              <CardContent className="space-y-3">
                {categorySummary.map(([category, amount]) => (
                  <div key={category} className="space-y-1">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">{categoryLabels[category] ?? category}</span>
                      <span className="font-medium tabular-nums">{formatCurrency(amount)}</span>
                    </div>
                    <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                      <div
                        className="h-full bg-destructive/50 rounded-full"
                        style={{ width: `${totalExpenses > 0 ? (amount / totalExpenses) * 100 : 0}%` }}
                      />
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader><CardTitle>{dict.bank.howToExport.title}</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-xs text-muted-foreground">
              <p><strong className="text-foreground">Itaú:</strong> {dict.bank.howToExport.itau}</p>
              <p><strong className="text-foreground">Bradesco:</strong> {dict.bank.howToExport.bradesco}</p>
              <p><strong className="text-foreground">Santander:</strong> {dict.bank.howToExport.santander}</p>
              <p><strong className="text-foreground">Nubank PJ:</strong> {dict.bank.howToExport.nubank}</p>
              <p><strong className="text-foreground">Sicoob/Sicredi:</strong> {dict.bank.howToExport.sicoob}</p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
