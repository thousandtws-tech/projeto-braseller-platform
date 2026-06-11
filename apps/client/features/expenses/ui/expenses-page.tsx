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
  OPERATIONAL: 'bg-blue-50 text-blue-700 ring-blue-200',
  PACKAGING: 'bg-sky-50 text-sky-700 ring-sky-200',
  SUPPLIES: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  LABOR: 'bg-violet-50 text-violet-700 ring-violet-200',
  BANK_FEE: 'bg-amber-50 text-amber-700 ring-amber-200',
  SHIPPING: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  TAX: 'bg-red-50 text-red-700 ring-red-200',
  OTHER: 'bg-slate-50 text-slate-600 ring-slate-200',
}

function currentMonthBounds() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()

  return {
    from: new Date(year, month, 1).toISOString().split('T')[0],
    to: new Date(year, month + 1, 0).toISOString().split('T')[0],
  }
}

const PAGE_SIZE = 20

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{
    from?: string
    to?: string
    category?: string
    page?: string
  }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)

  return {
    title: dict.expenses.title,
  }
}

export default async function DespesasPage({
  params,
  searchParams,
}: Props) {
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
  const periodTotal = items.reduce((sum, expense) => sum + Number(expense.amount), 0)

  function pageUrl(pageNumber: number) {
    const params = new URLSearchParams({
      from,
      to,
      ...(category ? { category } : {}),
      page: String(pageNumber),
    })

    return `/${lang}/despesas?${params}`
  }

  const byCategory = items.reduce<Record<string, number>>((acc, expense) => {
    acc[expense.category] = (acc[expense.category] ?? 0) + Number(expense.amount)
    return acc
  }, {})

  const topCategories = Object.entries(byCategory)
    .sort(([, first], [, second]) => second - first)
    .slice(0, 4)

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-6 py-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <h2 className="text-xl font-semibold tracking-tight text-slate-900">
            {dict.expenses.title}
          </h2>

          <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">
            {loadError
              ? loadError
              : totalCount > 0
                ? formatMessage(
                    totalCount === 1
                      ? dict.expenses.subtitleOne
                      : dict.expenses.subtitle,
                    {
                      count: totalCount,
                      total: formatCurrency(periodTotal),
                    }
                  )
                : dict.expenses.subtitleEmpty}
          </p>
        </div>

        <ExpenseFormSheet readOnly={readOnly} dict={dict} lang={lang} />
      </div>

      <ExpenseFilters from={from} to={to} category={category} dict={dict} lang={lang} />

      {topCategories.length > 0 && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {topCategories.map(([cat, amount]) => (
            <div
              key={cat}
              className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm shadow-slate-200/40"
            >
              <span
                className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ring-1 ${
                  CATEGORY_COLORS[cat] ?? 'bg-slate-50 text-slate-600 ring-slate-200'
                }`}
              >
                {dict.dre.categories[cat as keyof typeof dict.dre.categories] ?? cat}
              </span>

              <span className="shrink-0 text-sm font-semibold text-slate-900">
                {formatCurrency(amount)}
              </span>
            </div>
          ))}
        </div>
      )}

      <Card className="overflow-hidden rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40">
        <CardContent className="p-0">
          {loadError ? (
            <EmptyState
              title={dict.expenses.errors.loadError}
              description={loadError}
              destructive
            />
          ) : items.length === 0 ? (
            <EmptyState
              title={dict.expenses.empty.title}
              description={dict.expenses.empty.hint}
            />
          ) : (
            <div className="scroll-hidden overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50/80">
                  <tr className="border-b border-slate-100">
                    {[...Object.values(dict.expenses.table.columns), ''].map((header, index) => (
                      <th
                        key={index}
                        className="px-5 py-3 text-left text-xs font-medium text-slate-500"
                      >
                        {header}
                      </th>
                    ))}
                  </tr>
                </thead>

                <tbody>
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
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {totalPages > 1 && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <span className="text-xs text-slate-500">
            {formatMessage(dict.expenses.pagination.info, {
              current: currentPage,
              total: totalPages,
              count: totalCount,
            })}
          </span>

          <div className="flex items-center gap-2">
            {currentPage > 1 && (
              <Link
                href={pageUrl(currentPage - 1)}
                className="inline-flex h-9 items-center rounded-xl border border-slate-200 bg-white px-3 text-xs font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
              >
                {dict.expenses.pagination.previous}
              </Link>
            )}

            {currentPage < totalPages && (
              <Link
                href={pageUrl(currentPage + 1)}
                className="inline-flex h-9 items-center rounded-xl border border-slate-200 bg-white px-3 text-xs font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
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

function EmptyState({
  title,
  description,
  destructive = false,
}: {
  title: string
  description: string
  destructive?: boolean
}) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-16 text-center">
      <div
        className={
          destructive
            ? 'mb-3 size-2 rounded-full bg-red-500'
            : 'mb-3 size-2 rounded-full bg-slate-300'
        }
      />

      <p
        className={
          destructive
            ? 'text-sm font-semibold text-red-600'
            : 'text-sm font-semibold text-slate-700'
        }
      >
        {title}
      </p>

      <p className="mt-1 max-w-md text-xs leading-5 text-slate-500">
        {description}
      </p>
    </div>
  )
}