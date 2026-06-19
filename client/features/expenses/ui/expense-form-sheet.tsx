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

export function ExpenseFormSheet({ readOnly = false, dict, lang }: Props) {
  const router = useRouter()
  const [open, setOpen] = useState(false)
  const [formKey, setFormKey] = useState(0)

  function handleOpenChange(isOpen: boolean) {
    setOpen(isOpen)
    if (!isOpen) setFormKey((k) => k + 1)
  }

  const handleSuccess = useCallback((expenseDate?: string) => {
    const { from, to } = monthBounds(expenseDate)
    const params = new URLSearchParams({ from, to })

    setOpen(false)
    router.push(`/${lang}/despesas?${params}`)
    router.refresh()
  }, [router, lang])

  if (readOnly) {
    return (
      <div className="flex flex-col items-end gap-2">
        <button
          type="button"
          disabled
          className={cn(buttonVariants({ size: 'sm', variant: 'outline' }), 'gap-1.5 opacity-80')}
          title={dict.expenses.form.readOnlyTitle}
        >
          <LockKeyhole className="size-3.5 animate-pulse" />
          {dict.expenses.form.newExpense}
        </button>
        <ReadOnlyLock compact className="max-w-[260px]" />
      </div>
    )
  }

  return (
    <Sheet open={open} onOpenChange={handleOpenChange}>
      <SheetTrigger className={cn(buttonVariants({ size: 'lg' }), 'gap-2')}>
        <Plus className="size-3.5" />
        {dict.expenses.form.newExpense}
      </SheetTrigger>
      <SheetContent side="right" className="flex w-full flex-col sm:max-w-lg">
        <SheetHeader className="shrink-0 border-b p-6">
          <SheetTitle>{dict.expenses.form.sheetTitle}</SheetTitle>
          <SheetDescription>{dict.expenses.form.sheetDescription}</SheetDescription>
        </SheetHeader>
        <div className="flex-1 overflow-y-auto p-6">
          <ExpenseForm key={formKey} onSuccess={handleSuccess} dict={dict} />
        </div>
      </SheetContent>
    </Sheet>
  )
}
