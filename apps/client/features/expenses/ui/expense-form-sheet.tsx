'use client'

import { useCallback, useState } from 'react'
import { useRouter } from 'next/navigation'
import { LockKeyhole, Plus } from 'lucide-react'

import { cn } from '@/shared/lib/utils'
import { buttonVariants } from '@/shared/ui/button'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/shared/ui/sheet'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

import { ExpenseForm } from './expense-form'

function monthBounds(dateValue?: string) {
  const date = dateValue ? new Date(`${dateValue}T00:00:00`) : new Date()
  const year = date.getFullYear()
  const month = date.getMonth()

  return {
    from: new Date(year, month, 1).toISOString().split('T')[0],
    to: new Date(year, month + 1, 0).toISOString().split('T')[0],
  }
}

interface Props {
  readOnly?: boolean
  dict: Dictionary
  lang: Locale
}

export function ExpenseFormSheet({
  readOnly = false,
  dict,
  lang,
}: Props) {
  const router = useRouter()
  const [open, setOpen] = useState(false)
  const [formKey, setFormKey] = useState(0)

  function handleOpenChange(isOpen: boolean) {
    setOpen(isOpen)

    if (!isOpen) {
      setFormKey((current) => current + 1)
    }
  }

  const handleSuccess = useCallback(
    (expenseDate?: string) => {
      const { from, to } = monthBounds(expenseDate)
      const params = new URLSearchParams({ from, to })

      setOpen(false)
      router.push(`/${lang}/despesas?${params}`)
      router.refresh()
    },
    [router, lang]
  )

  if (readOnly) {
    return (
      <div className="flex flex-col items-end gap-2">
        <button
          type="button"
          disabled
          className={cn(
            buttonVariants({ size: 'sm', variant: 'outline' }),
            'h-10 gap-2 rounded-xl border-slate-200 bg-slate-50 px-4 text-sm font-medium text-slate-400 opacity-80 shadow-sm'
          )}
          title={dict.expenses.form.readOnlyTitle}
        >
          <LockKeyhole className="size-4" />
          {dict.expenses.form.newExpense}
        </button>

        <ReadOnlyLock compact className="max-w-[260px]" />
      </div>
    )
  }

  return (
    <Sheet open={open} onOpenChange={handleOpenChange}>
      <SheetTrigger
        className={cn(
          buttonVariants({ size: 'sm' }),
          'h-10 gap-2 rounded-xl bg-blue-600 px-4 text-sm font-medium text-white shadow-sm shadow-blue-600/20 transition hover:bg-blue-700 hover:shadow-md'
        )}
      >
        <Plus className="size-4" />
        {dict.expenses.form.newExpense}
      </SheetTrigger>

      <SheetContent
        side="right"
        className="flex w-full flex-col border-l border-slate-200 bg-white p-0 shadow-2xl shadow-slate-900/10 sm:max-w-md"
      >
        <SheetHeader className="shrink-0 border-b border-slate-100 px-6 py-5 text-left">
          <div className="flex items-start gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-50">
              <Plus className="size-4 text-blue-600" />
            </div>

            <div className="min-w-0">
              <SheetTitle className="text-base font-semibold tracking-tight text-slate-900">
                {dict.expenses.form.sheetTitle}
              </SheetTitle>

              <SheetDescription className="mt-1 text-sm leading-5 text-slate-500">
                {dict.expenses.form.sheetDescription}
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="scroll-hidden flex-1 overflow-y-auto px-6 py-5">
          <ExpenseForm key={formKey} onSuccess={handleSuccess} dict={dict} />
        </div>
      </SheetContent>
    </Sheet>
  )
}