'use client'

import { useTransition } from 'react'
import { Trash2, Paperclip, Loader2 } from 'lucide-react'
import { deleteExpenseAction } from '@/features/reports/server/actions'
import { formatCurrency, formatDate } from '@/shared/api/gateway'
import { ReadOnlyLockInline } from '@/shared/ui/read-only-lock'
import type { ExpenseEntry } from '@/shared/types'

const CATEGORY_COLORS: Record<string, string> = {
  OPERATIONAL:  'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  PACKAGING:    'bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-400',
  SUPPLIES:     'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  LABOR:        'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  BANK_FEE:     'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  SHIPPING:     'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400',
  TAX:          'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  OTHER:        'bg-muted text-muted-foreground',
  // Legacy Portuguese names
  Embalagem:    'bg-blue-100 text-blue-700',
  Frete:        'bg-purple-100 text-purple-700',
  Produto:      'bg-green-100 text-green-700',
  Plataforma:   'bg-indigo-100 text-indigo-700',
  Contabilidade:'bg-gray-100 text-gray-700',
  Outros:       'bg-muted text-muted-foreground',
}

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

export function ExpenseRow({ expense, readOnly = false }: { expense: ExpenseEntry; readOnly?: boolean }) {
  const [isPending, startTransition] = useTransition()
  const label = CATEGORY_LABELS[expense.category] ?? expense.category
  const color = CATEGORY_COLORS[expense.category] ?? CATEGORY_COLORS.OTHER

  function handleDelete() {
    if (!confirm('Excluir esta despesa?')) return
    startTransition(() => deleteExpenseAction(expense.id))
  }

  return (
    <tr className={`border-b border-border/50 hover:bg-muted/30 transition-colors group ${isPending ? 'opacity-50' : ''}`}>
      <td className="px-4 py-3 text-muted-foreground whitespace-nowrap text-sm">
        {formatDate(expense.expenseDate)}
      </td>
      <td className="px-4 py-3 font-medium text-sm">{expense.description}</td>
      <td className="px-4 py-3">
        <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ${color}`}>
          {label}
        </span>
      </td>
      <td className="px-4 py-3 font-medium whitespace-nowrap text-sm">
        {formatCurrency(expense.amount)}
      </td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          {expense.attachmentUrl && (
            <a
              href={expense.attachmentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="p-1 rounded hover:text-primary transition-colors"
              title="Ver comprovante"
            >
              <Paperclip className="size-3.5" />
            </a>
          )}
          {readOnly ? (
            <ReadOnlyLockInline />
          ) : (
            <button
              onClick={handleDelete}
              disabled={isPending}
              className="p-1 rounded hover:text-destructive transition-colors disabled:opacity-50"
              title="Excluir"
            >
              {isPending
                ? <Loader2 className="size-3.5 animate-spin" />
                : <Trash2 className="size-3.5" />
              }
            </button>
          )}
        </div>
      </td>
    </tr>
  )
}
