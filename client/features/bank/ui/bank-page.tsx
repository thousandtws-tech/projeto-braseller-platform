import { ArrowDownLeft, ArrowUpRight, Building2, FileUp, Landmark, ShieldCheck, Tags } from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/ui/table'
import { getToken, getSession } from '@/entities/session/server/session'
import { formatCurrency, getBankTransactions } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import type { BankTransaction } from '@/shared/types'
import { UploadOfxForm } from './upload-ofx-form'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

function currentMonthBounds() {
  const now = new Date()
  return {
    from: new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0],
    to: new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0],
  }
}

function summarizeByCategory(transactions: BankTransaction[]) {
  return Object.entries(
    transactions
      .filter((transaction) => transaction.tran_type === 'DEBIT')
      .reduce<Record<string, number>>((acc, transaction) => {
        acc[transaction.category] = (acc[transaction.category] ?? 0) + transaction.amount
        return acc
      }, {})
  ).sort((a, b) => b[1] - a[1])
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
  const [dict, token, session] = await Promise.all([
    getDictionary(lang),
    getToken().then((value) => value ?? ''),
    getSession(),
  ])
  const categoryLabels = dict.bank.categories as Record<string, string>
  const tenantId = session?.tenantId ?? ''
  const readOnly = isReadOnlyAccountant(session?.roles)
  const { from, to } = currentMonthBounds()
  const transactions = tenantId ? await getBankTransactions(token, tenantId, from, to) : []
  const debits = transactions.filter((transaction) => transaction.tran_type === 'DEBIT')
  const credits = transactions.filter((transaction) => transaction.tran_type !== 'DEBIT')
  const totalExpenses = debits.reduce((sum, transaction) => sum + transaction.amount, 0)
  const totalCredits = credits.reduce((sum, transaction) => sum + transaction.amount, 0)
  const categorySummary = summarizeByCategory(transactions)
  const netMovement = totalCredits - totalExpenses

  return (
    <div className="flex w-full flex-col gap-6">
      <header>
        <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.bank.header.title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{dict.bank.header.subtitle}</p>
      </header>

      <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
        <Metric label="Entradas identificadas" value={formatCurrency(totalCredits)} helper={`${credits.length} créditos`} icon={ArrowDownLeft} />
        <Metric label={dict.bank.sidebar.totalExpenses} value={formatCurrency(totalExpenses)} helper={`${debits.length} débitos`} icon={ArrowUpRight} />
        <Metric label="Movimento líquido" value={formatCurrency(netMovement)} helper="Entradas menos saídas" icon={Landmark} />
        <Metric label="Categorias" value={String(categorySummary.length)} helper={`${transactions.length} movimentos`} icon={Tags} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
        <div className="flex min-w-0 flex-col gap-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2"><FileUp className="size-4" />{dict.bank.importOfx.title}</CardTitle>
              <p className="text-xs leading-5 text-muted-foreground">{dict.bank.importOfx.hint}</p>
            </CardHeader>
            <CardContent><UploadOfxForm readOnly={readOnly} dict={dict} /></CardContent>
          </Card>

          <Card className="overflow-hidden">
            <CardHeader className="flex-row items-center justify-between gap-4">
              <div>
                <CardTitle>{dict.bank.transactions.title}</CardTitle>
                <p className="mt-1 text-xs text-muted-foreground">{formatMessage(dict.bank.transactions.count, { count: transactions.length })}</p>
              </div>
              <Badge variant="secondary">Mês atual</Badge>
            </CardHeader>
            <CardContent className="p-0">
              {transactions.length === 0 ? (
                <div className="flex min-h-64 flex-col items-center justify-center gap-3 px-6 text-center">
                  <div className="flex size-11 items-center justify-center rounded-full border border-border bg-muted/40"><Landmark className="size-5 text-muted-foreground" /></div>
                  <div><p className="font-medium">Nenhuma transação importada</p><p className="mt-1 max-w-md text-sm leading-6 text-muted-foreground">{dict.bank.transactions.empty}</p></div>
                </div>
              ) : (
                <Table>
                  <TableHeader className="bg-muted/70">
                    <TableRow className="hover:bg-muted/70">
                      <TableHead className="pl-5">{dict.bank.transactions.columns.date}</TableHead>
                      <TableHead>{dict.bank.transactions.columns.description}</TableHead>
                      <TableHead>{dict.bank.transactions.columns.category}</TableHead>
                      <TableHead className="pr-5 text-right">{dict.bank.transactions.columns.value}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {transactions.map((transaction) => (
                      <TableRow key={transaction.id}>
                        <TableCell className="pl-5 text-xs text-muted-foreground">{new Date(transaction.posted_date).toLocaleDateString(LOCALE_MAP[lang])}</TableCell>
                        <TableCell className="max-w-80 font-medium"><p className="truncate">{transaction.description ?? '—'}</p></TableCell>
                        <TableCell><Badge variant="secondary">{categoryLabels[transaction.category] ?? transaction.category}</Badge></TableCell>
                        <TableCell className="pr-5 text-right font-semibold">
                          <span className={transaction.tran_type === 'DEBIT' ? 'text-destructive' : 'text-foreground'}>
                            {transaction.tran_type === 'DEBIT' ? '− ' : '+ '}{formatCurrency(transaction.amount)}
                          </span>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </div>

        <aside className="flex flex-col gap-6">
          <Card>
            <CardHeader><CardTitle>{dict.bank.sidebar.byCategory}</CardTitle></CardHeader>
            <CardContent className="flex flex-col gap-4">
              {categorySummary.length === 0 ? (
                <p className="text-sm leading-6 text-muted-foreground">As categorias aparecerão depois da primeira importação.</p>
              ) : categorySummary.map(([category, amount]) => (
                <div key={category} className="flex flex-col gap-2">
                  <div className="flex items-center justify-between gap-3 text-sm">
                    <span className="truncate text-muted-foreground">{categoryLabels[category] ?? category}</span>
                    <span className="font-semibold tabular-nums">{formatCurrency(amount)}</span>
                  </div>
                  <div className="h-1 overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full bg-foreground/55" style={{ width: `${totalExpenses > 0 ? (amount / totalExpenses) * 100 : 0}%` }} />
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle className="flex items-center gap-2"><Building2 className="size-4" />{dict.bank.howToExport.title}</CardTitle></CardHeader>
            <CardContent className="flex flex-col gap-4 text-xs leading-5 text-muted-foreground">
              {[
                ['Itaú', dict.bank.howToExport.itau],
                ['Bradesco', dict.bank.howToExport.bradesco],
                ['Santander', dict.bank.howToExport.santander],
                ['Nubank PJ', dict.bank.howToExport.nubank],
                ['Sicoob/Sicredi', dict.bank.howToExport.sicoob],
              ].map(([bank, instruction], index) => (
                <div key={bank} className="flex gap-3">
                  <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-muted text-[10px] font-semibold text-foreground">{index + 1}</span>
                  <p><strong className="text-foreground">{bank}:</strong> {instruction}</p>
                </div>
              ))}
            </CardContent>
          </Card>
        </aside>
      </section>
    </div>
  )
}

function Metric({ label, value, helper, icon: Icon }: { label: string; value: string; helper: string; icon: React.ComponentType<{ className?: string }> }) {
  return (
    <div className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0">
      <div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div>
      <p className="text-2xl font-semibold tracking-[-0.035em] tabular-nums">{value}</p>
      <p className="text-[11px] text-muted-foreground">{helper}</p>
    </div>
  )
}
