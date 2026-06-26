import Link from 'next/link'
import { ArrowLeft, ArrowRight, FileText, Layers3, Receipt, WalletCards } from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import {
  Table,
  TableBody,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/ui/table'
import { getToken, getSession } from '@/entities/session/server/session'
import { formatCurrency, getReportsExpenses } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import { ExpenseRow } from './expense-row'
import { ExpenseFormSheet } from './expense-form-sheet'
import { ExpenseFilters } from './expense-filters'

function currentMonthBounds() {
  const now = new Date()
  return {
    from: new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0],
    to: new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0],
  }
}

const PAGE_SIZE = 20

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ from?: string; to?: string; category?: string; page?: string }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.expenses.title }
}

export default async function DespesasPage({ params, searchParams }: Props) {
  const { lang } = await params
  const [dict, sp, token, session] = await Promise.all([
    getDictionary(lang),
    searchParams,
    getToken().then((value) => value ?? ''),
    getSession(),
  ])
  const defaults = currentMonthBounds()
  const from = sp.from ?? defaults.from
  const to = sp.to ?? defaults.to
  const category = sp.category ?? ''
  const page = Math.max(0, (parseInt(sp.page ?? '1') || 1) - 1)
  const readOnly = isReadOnlyAccountant(session?.roles)

  let items: Awaited<ReturnType<typeof getReportsExpenses>>['items'] = []
  let totalCount = 0
  let loadError: string | null = null

  if (session?.tenantId) {
    try {
      const data = await getReportsExpenses(token, session.tenantId, {
        from,
        to,
        page,
        size: PAGE_SIZE,
        ...(category ? { category } : {}),
      })
      items = data.items ?? []
      totalCount = data.total ?? 0
    } catch {
      loadError = dict.expenses.errors.loadFailed
    }
  } else {
    loadError = dict.expenses.errors.noTenant
  }

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
  const currentPage = page + 1
  const periodTotal = items.reduce((sum, expense) => sum + Number(expense.amount), 0)
  const byCategory = items.reduce<Record<string, number>>((acc, expense) => {
    acc[expense.category] = (acc[expense.category] ?? 0) + Number(expense.amount)
    return acc
  }, {})
  const categories = Object.entries(byCategory).sort(([, a], [, b]) => b - a)
  const averageExpense = items.length > 0 ? periodTotal / items.length : 0
  const mainCategory = categories[0]

  function pageUrl(nextPage: number) {
    const query = new URLSearchParams({
      from,
      to,
      ...(category ? { category } : {}),
      page: String(nextPage),
    })
    return `/${lang}/despesas?${query}`
  }

  return (
    <div className="flex w-full flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.expenses.title}</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Registre custos operacionais e acompanhe o impacto no resultado.
          </p>
        </div>
        <ExpenseFormSheet readOnly={readOnly} dict={dict} lang={lang} />
      </header>

      <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
        <Metric label="Total no período" value={formatCurrency(periodTotal)} helper={`${totalCount} registros`} icon={WalletCards} />
        <Metric label="Despesa média" value={formatCurrency(averageExpense)} helper="Por lançamento exibido" icon={Receipt} />
        <Metric label="Categorias" value={String(categories.length)} helper="Com movimentação" icon={Layers3} />
        <Metric
          label="Maior categoria"
          value={mainCategory ? formatCurrency(mainCategory[1]) : '—'}
          helper={mainCategory ? dict.expenses.categories[mainCategory[0] as keyof typeof dict.expenses.categories] ?? mainCategory[0] : 'Sem dados'}
          icon={FileText}
        />
      </section>

      <section className="rounded-lg border border-border bg-card p-4">
        <div className="mb-3">
          <p className="text-xs font-semibold uppercase tracking-[0.08em]">Período e categoria</p>
          <p className="mt-1 text-xs text-muted-foreground">A lista e os indicadores acompanham a seleção abaixo.</p>
        </div>
        <ExpenseFilters from={from} to={to} category={category} dict={dict} lang={lang} />
      </section>

      {categories.length > 0 ? (
        <section className="flex flex-wrap gap-2">
          {categories.slice(0, 5).map(([key, amount]) => (
            <div key={key} className="flex items-center gap-3 rounded-md border border-border bg-card px-3 py-2">
              <Badge variant="secondary">{dict.expenses.categories[key as keyof typeof dict.expenses.categories] ?? key}</Badge>
              <span className="text-xs font-semibold tabular-nums">{formatCurrency(amount)}</span>
            </div>
          ))}
        </section>
      ) : null}

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between gap-4">
          <div>
            <CardTitle>Despesas registradas</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">
              {loadError
                ? loadError
                : totalCount > 0
                  ? formatMessage(totalCount === 1 ? dict.expenses.subtitleOne : dict.expenses.subtitle, {
                      count: totalCount,
                      total: formatCurrency(periodTotal),
                    })
                  : dict.expenses.subtitleEmpty}
            </p>
          </div>
          {category ? <Badge variant="outline">{dict.expenses.categories[category as keyof typeof dict.expenses.categories] ?? category}</Badge> : null}
        </CardHeader>
        <CardContent className="p-0">
          {loadError ? (
            <StateMessage title={dict.expenses.errors.loadError} description={loadError} destructive />
          ) : items.length === 0 ? (
            <StateMessage title={dict.expenses.empty.title} description={dict.expenses.empty.hint} />
          ) : (
            <Table>
              <TableHeader className="bg-muted/70">
                <TableRow className="hover:bg-muted/70">
                  <TableHead className="pl-5">{dict.expenses.table.columns.date}</TableHead>
                  <TableHead>{dict.expenses.table.columns.description}</TableHead>
                  <TableHead>{dict.expenses.table.columns.category}</TableHead>
                  <TableHead className="text-right">{dict.expenses.table.columns.value}</TableHead>
                  <TableHead className="w-20 pr-5 text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((expense) => (
                  <ExpenseRow
                    key={expense.id}
                    readOnly={readOnly}
                    dict={dict}
                    expense={{
                      id: expense.id,
                      expenseDate: expense.expense_date,
                      category: expense.category,
                      description: expense.description,
                      amount: Number(expense.amount),
                      attachmentUrl: expense.attachment?.secure_url,
                    }}
                  />
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {totalPages > 1 ? (
        <nav className="flex items-center justify-between gap-3">
          <span className="text-xs text-muted-foreground">
            {formatMessage(dict.expenses.pagination.info, { current: currentPage, total: totalPages, count: totalCount })}
          </span>
          <div className="flex items-center gap-2">
            <PageLink href={pageUrl(currentPage - 1)} disabled={currentPage <= 1} label={dict.expenses.pagination.previous} icon={ArrowLeft} />
            <span className="flex size-9 items-center justify-center rounded-md bg-foreground text-xs font-semibold text-background">{currentPage}</span>
            <PageLink href={pageUrl(currentPage + 1)} disabled={currentPage >= totalPages} label={dict.expenses.pagination.next} icon={ArrowRight} trailing />
          </div>
        </nav>
      ) : null}
    </div>
  )
}

function Metric({ label, value, helper, icon: Icon }: { label: string; value: string; helper: string; icon: React.ComponentType<{ className?: string }> }) {
  return (
    <div className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0">
      <div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div>
      <p className="text-2xl font-semibold tracking-[-0.035em] tabular-nums">{value}</p>
      <p className="truncate text-[11px] text-muted-foreground">{helper}</p>
    </div>
  )
}

function StateMessage({ title, description, destructive = false }: { title: string; description: string; destructive?: boolean }) {
  return (
    <div className="flex min-h-64 flex-col items-center justify-center gap-3 px-6 text-center">
      <div className="flex size-11 items-center justify-center rounded-full border border-border bg-muted/40"><FileText className="size-5 text-muted-foreground" /></div>
      <div><p className={destructive ? 'font-medium text-destructive' : 'font-medium'}>{title}</p><p className="mt-1 max-w-md text-sm text-muted-foreground">{description}</p></div>
    </div>
  )
}

function PageLink({ href, disabled, label, icon: Icon, trailing = false }: { href: string; disabled: boolean; label: string; icon: React.ComponentType<{ className?: string }>; trailing?: boolean }) {
  return (
    <Link href={disabled ? '#' : href} aria-disabled={disabled} className={disabled ? 'pointer-events-none inline-flex h-9 items-center gap-2 rounded-md border border-border px-3 text-sm text-muted-foreground opacity-50' : 'inline-flex h-9 items-center gap-2 rounded-md border border-input bg-background px-3 text-sm font-medium hover:bg-muted'}>
      {trailing ? null : <Icon className="size-4" />}<span className="hidden sm:inline">{label}</span>{trailing ? <Icon className="size-4" /> : null}
    </Link>
  )
}
