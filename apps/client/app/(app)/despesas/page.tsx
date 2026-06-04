import type { Metadata } from 'next'
import Link from 'next/link'
import { Card, CardContent } from '@/components/ui/card'
import { getToken, getSession } from '@/lib/auth'
import { getReportsExpenses, formatCurrency } from '@/lib/api'
import { ExpenseRow } from './expense-row'
import { ExpenseFormSheet } from './expense-form-sheet'
import { ExpenseFilters } from './expense-filters'

export const metadata: Metadata = { title: 'Despesas' }

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

export default async function DespesasPage({
  searchParams,
}: {
  searchParams: Promise<{ from?: string; to?: string; category?: string; page?: string }>
}) {
  const params = await searchParams
  const defaults = currentMonthBounds()
  const from = params.from ?? defaults.from
  const to = params.to ?? defaults.to
  const category = params.category ?? ''
  const page = Math.max(0, (parseInt(params.page ?? '1') || 1) - 1)

  const token = (await getToken()) ?? ''
  const session = await getSession()

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
      loadError = 'Não foi possível carregar as despesas cadastradas. Verifique a conexão com a API.'
    }
  } else {
    loadError = 'Não foi possível identificar a empresa da sessão.'
  }

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
  const currentPage = page + 1
  const periodTotal = items.reduce((s, e) => s + Number(e.amount), 0)

  function pageUrl(p: number) {
    const sp = new URLSearchParams({
      from,
      to,
      ...(category ? { category } : {}),
      page: String(p),
    })
    return `/despesas?${sp}`
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
          <h2 className="text-xl font-semibold">Despesas</h2>
          <p className="text-sm text-muted-foreground">
            {loadError
              ? loadError
              : totalCount > 0
                ? `${totalCount} registro${totalCount !== 1 ? 's' : ''} · Total: ${formatCurrency(periodTotal)}`
                : 'Nenhuma despesa no período selecionado'}
          </p>
        </div>
        <ExpenseFormSheet />
      </div>

      {/* Filters */}
      <ExpenseFilters from={from} to={to} category={category} />

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
                {CATEGORY_LABELS[cat] ?? cat}
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
              <p className="text-sm font-medium text-destructive">Erro ao carregar despesas</p>
              <p className="text-xs text-muted-foreground/60 mt-1 max-w-md">
                {loadError}
              </p>
            </div>
          ) : items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <p className="text-sm font-medium text-muted-foreground">Nenhuma despesa encontrada</p>
              <p className="text-xs text-muted-foreground/60 mt-1">
                Ajuste os filtros ou registre uma nova despesa.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border bg-muted/40">
                    {['Data', 'Descrição', 'Categoria', 'Valor', ''].map((h) => (
                      <th
                        key={h}
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
            Página {currentPage} de {totalPages} · {totalCount} registros
          </span>
          <div className="flex items-center gap-1.5">
            {currentPage > 1 && (
              <Link
                href={pageUrl(currentPage - 1)}
                className="inline-flex items-center rounded-md border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted transition-colors"
              >
                Anterior
              </Link>
            )}
            {currentPage < totalPages && (
              <Link
                href={pageUrl(currentPage + 1)}
                className="inline-flex items-center rounded-md border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted transition-colors"
              >
                Próxima
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
