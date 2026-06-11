import Link from 'next/link'
import { Card, CardContent } from '@/shared/ui/card'
import { getToken, getSession } from '@/entities/session/server/session'
import { getReportsExpenses, formatCurrency } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import { ExpenseRow } from './expense-row'
import { ExpenseFormSheet } from './expense-form-sheet'
import { ExpenseFilters } from './expense-filters'

const CATEGORY_COLORS: Record<string, string> = {
  OPERATIONAL: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  PACKAGING: 'bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-400',
  SUPPLIES: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  LABOR: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  BANK_FEE: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  SHIPPING: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400',
  TAX: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  OTHER: 'bg-muted text-muted-foreground',
}

function currentMonthBounds() {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  return {
    from: new Date(y, m, 1).toISOString().split('T')[0],
    to: new Date(y, m + 1, 0).toISOString().split('T')[0],
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
  const dict = await getDictionary(lang)
  const sp = await searchParams
  const defaults = currentMonthBounds()
  const from = sp.from ?? defaults.from
  const to = sp.to ?? defaults.to
  const category = sp.category ?? ''
  const page = Math.max(0, (parseInt(sp.page ?? '1') || 1) - 1)

  const token = (await getToken()) ?? ''
  const session = await getSession()
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
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error)
      console.warn('[despesas] Failed to load expenses', {
        tenantId: session.tenantId,
        from,
        to,
        category: category || undefined,
        page,
        error: errorMessage,
      })
      loadError = dict.expenses.errors.loadFailed
    }
  } else {
    loadError = dict.expenses.errors.noTenant
  }

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
  const currentPage = page + 1
  const periodTotal = items.reduce((s, e) => s + Number(e.amount), 0)

  function pageUrl(p: number) {
    const params = new URLSearchParams({
      from,
      to,
      ...(category ? { category } : {}),
      page: String(p),
    })
    return `/${lang}/despesas?${params}`
  }

  // Group items by category for summary
  const byCategory = items.reduce<Record<string, number>>((acc, e) => {
    acc[e.category] = (acc[e.category] ?? 0) + Number(e.amount)
    return acc
  }, {})
  const topCategories = Object.entries(byCategory)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 4)

  return (
    <div className="space-y-5 max-w-6xl">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">{dict.expenses.title}</h2>
          <p className="text-sm text-muted-foreground">
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
        <ExpenseFormSheet readOnly={readOnly} dict={dict} lang={lang} />
      </div>

      {/* Filters */}
      <ExpenseFilters from={from} to={to} category={category} dict={dict} lang={lang} />

      {/* Category summary strip */}
      {topCategories.length > 0 && (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {topCategories.map(([cat, amount]) => (
            <div
              key={cat}
              className="flex items-center justify-between rounded-lg border border-border bg-card px-3 py-2"
            >
              <span
                className={`text-xs font-medium rounded px-1.5 py-0.5 ${CATEGORY_COLORS[cat] ?? 'bg-muted text-muted-foreground'}`}
              >
                {dict.dre.categories[cat as keyof typeof dict.dre.categories] ?? cat}
              </span>
              <span className="text-xs font-semibold text-foreground">
                {formatCurrency(amount)}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Expenses table */}
      <Card>
        <CardContent className="p-0">
          {loadError ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <p className="text-sm font-medium text-destructive">{dict.expenses.errors.loadError}</p>
              <p className="text-xs text-muted-foreground/60 mt-1 max-w-md">
                {loadError}
              </p>
            </div>
          ) : items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <p className="text-sm font-medium text-muted-foreground">{dict.expenses.empty.title}</p>
              <p className="text-xs text-muted-foreground/60 mt-1">
                {dict.expenses.empty.hint}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border bg-muted/40">
                    {[...Object.values(dict.expenses.table.columns), ''].map((h, i) => (
                      <th
                        key={i}
                        className="text-left px-4 py-2.5 text-xs font-medium text-muted-foreground"
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {items.map((e) => (
                    <ExpenseRow
                      key={e.id}
                      readOnly={readOnly}
                      dict={dict}
                      expense={{
                        id: e.id,
                        expenseDate: e.expense_date,
                        category: e.category,
                        description: e.description,
                        amount: Number(e.amount),
                        attachmentUrl: e.attachment?.secure_url,
                      }}
                    />
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
            {formatMessage(dict.expenses.pagination.info, { current: currentPage, total: totalPages, count: totalCount })}
          </span>
          <div className="flex items-center gap-1.5">
            {currentPage > 1 && (
              <Link
                href={pageUrl(currentPage - 1)}
                className="inline-flex items-center rounded-md border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted transition-colors"
              >
                {dict.expenses.pagination.previous}
              </Link>
            )}
            {currentPage < totalPages && (
              <Link
                href={pageUrl(currentPage + 1)}
                className="inline-flex items-center rounded-md border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted transition-colors"
              >
                {dict.expenses.pagination.next}
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
