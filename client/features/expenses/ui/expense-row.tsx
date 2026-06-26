'use client'

import { useTransition } from 'react'
import { Trash2, Paperclip, Loader2 } from 'lucide-react'
import { Badge } from '@/shared/ui/badge'
import { TableCell, TableRow } from '@/shared/ui/table'
import { deleteExpenseAction } from '@/features/reports/server/actions'
import { formatCurrency, formatDate } from '@/shared/api/gateway'
import { ReadOnlyLockInline } from '@/shared/ui/read-only-lock'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { ExpenseEntry } from '@/shared/types'

interface Props {
  expense: ExpenseEntry
  readOnly?: boolean
  dict: Dictionary
}

export function ExpenseRow({ expense, readOnly = false, dict }: Props) {
  const [isPending, startTransition] = useTransition()
  const label = dict.expenses.categories[expense.category as keyof typeof dict.expenses.categories] ?? expense.category

  function handleDelete() {
    if (!confirm(dict.expenses.row.confirmDelete)) return
    startTransition(() => deleteExpenseAction(expense.id))
  }

  return (
    <TableRow className={isPending ? 'group opacity-50' : 'group'}>
      <TableCell className="pl-5 text-xs text-muted-foreground">
        {formatDate(expense.expenseDate)}
      </TableCell>
      <TableCell className="font-medium">{expense.description}</TableCell>
      <TableCell><Badge variant="secondary">{label}</Badge></TableCell>
      <TableCell className="text-right font-semibold">
        {formatCurrency(expense.amount)}
      </TableCell>
      <TableCell className="pr-5">
        <div className="flex items-center justify-end gap-1 opacity-60 transition-opacity group-hover:opacity-100">
          {expense.attachmentUrl && (
            <a
              href={expense.attachmentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="p-1 rounded hover:text-primary transition-colors"
              title={dict.expenses.row.viewReceipt}
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
              title={dict.expenses.row.delete}
            >
              {isPending
                ? <Loader2 className="size-3.5 animate-spin" />
                : <Trash2 className="size-3.5" />
              }
            </button>
          )}
        </div>
      </TableCell>
    </TableRow>
  )
}
