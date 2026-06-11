'use client'

import { useTransition } from 'react'
import {
  Trash2,
  Paperclip,
  Loader2,
  FileText,
} from 'lucide-react'

import { deleteExpenseAction } from '@/features/reports/server/actions'
import { formatCurrency, formatDate } from '@/shared/api/gateway'
import { ReadOnlyLockInline } from '@/shared/ui/read-only-lock'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { ExpenseEntry } from '@/shared/types'

const CATEGORY_COLORS: Record<string, string> = {
  OPERATIONAL: 'bg-blue-50 text-blue-700 ring-blue-200',
  PACKAGING: 'bg-sky-50 text-sky-700 ring-sky-200',
  SUPPLIES: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  LABOR: 'bg-violet-50 text-violet-700 ring-violet-200',
  BANK_FEE: 'bg-amber-50 text-amber-700 ring-amber-200',
  SHIPPING: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  TAX: 'bg-red-50 text-red-700 ring-red-200',
  OTHER: 'bg-slate-50 text-slate-600 ring-slate-200',

  Embalagem: 'bg-sky-50 text-sky-700 ring-sky-200',
  Frete: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  Produto: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  Plataforma: 'bg-violet-50 text-violet-700 ring-violet-200',
  Contabilidade: 'bg-slate-50 text-slate-700 ring-slate-200',
  Outros: 'bg-slate-50 text-slate-600 ring-slate-200',
}

interface Props {
  expense: ExpenseEntry
  readOnly?: boolean
  dict: Dictionary
}

export function ExpenseRow({
  expense,
  readOnly = false,
  dict,
}: Props) {
  const [isPending, startTransition] = useTransition()

  const label =
    dict.dre.categories[
      expense.category as keyof typeof dict.dre.categories
    ] ?? expense.category

  const color =
    CATEGORY_COLORS[expense.category] ??
    CATEGORY_COLORS.OTHER

  function handleDelete() {
    if (!confirm(dict.expenses.row.confirmDelete)) {
      return
    }

    startTransition(() => {
      deleteExpenseAction(expense.id)
    })
  }

  return (
    <tr
      className={`
        border-b border-slate-100
        transition-colors
        hover:bg-slate-50/70
        ${isPending ? 'opacity-50' : ''}
      `}
    >
      {/* Data */}
      <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-500">
        {formatDate(expense.expenseDate)}
      </td>

      {/* Descrição */}
      <td className="px-5 py-4">
        <div className="flex flex-col">
          <span className="font-medium text-slate-900">
            {expense.description}
          </span>

          {expense.attachmentUrl && (
            <span className="mt-1 flex items-center gap-1 text-xs text-slate-400">
              <FileText className="size-3" />
              Comprovante anexado
            </span>
          )}
        </div>
      </td>

      {/* Categoria */}
      <td className="px-5 py-4">
        <span
          className={`
            inline-flex items-center
            rounded-full
            px-2.5
            py-1
            text-xs
            font-medium
            ring-1
            ${color}
          `}
        >
          {label}
        </span>
      </td>

      {/* Valor */}
      <td className="whitespace-nowrap px-5 py-4">
        <span className="text-sm font-semibold text-slate-900">
          {formatCurrency(expense.amount)}
        </span>
      </td>

      {/* Ações */}
      <td className="px-5 py-4">
        <div className="flex items-center gap-1">
          {expense.attachmentUrl && (
            <a
              href={expense.attachmentUrl}
              target="_blank"
              rel="noopener noreferrer"
              title={dict.expenses.row.viewReceipt}
              className="
                flex size-8 items-center justify-center
                rounded-lg
                text-slate-500
                transition-all
                hover:bg-blue-50
                hover:text-blue-600
              "
            >
              <Paperclip className="size-4" />
            </a>
          )}

          {readOnly ? (
            <div className="ml-1">
              <ReadOnlyLockInline />
            </div>
          ) : (
            <button
              type="button"
              onClick={handleDelete}
              disabled={isPending}
              title={dict.expenses.row.delete}
              className="
                flex size-8 items-center justify-center
                rounded-lg
                text-slate-500
                transition-all
                hover:bg-red-50
                hover:text-red-600
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
            >
              {isPending ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <Trash2 className="size-4" />
              )}
            </button>
          )}
        </div>
      </td>
    </tr>
  )
}