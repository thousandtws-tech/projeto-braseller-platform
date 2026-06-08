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

export function ExpenseFormSheet({ readOnly = false }: { readOnly?: boolean }) {
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
    router.push(`/despesas?${params}`)
    router.refresh()
  }, [router])

  if (readOnly) {
    return (
      <div className="flex flex-col items-end gap-2">
        <button
          type="button"
          disabled
          className={cn(buttonVariants({ size: 'sm', variant: 'outline' }), 'gap-1.5 opacity-80')}
          title="Contador possui acesso somente para visualizacao"
        >
          <LockKeyhole className="size-3.5 animate-pulse" />
          Nova Despesa
        </button>
        <ReadOnlyLock compact className="max-w-[260px]" />
      </div>
    )
  }

  return (
    <Sheet open={open} onOpenChange={handleOpenChange}>
      <SheetTrigger className={cn(buttonVariants({ size: 'sm' }), 'gap-1.5')}>
        <Plus className="size-3.5" />
        Nova Despesa
      </SheetTrigger>
      <SheetContent side="right" className="flex w-full flex-col sm:max-w-md">
        <SheetHeader className="shrink-0">
          <SheetTitle>Nova Despesa</SheetTitle>
          <SheetDescription>Preencha os dados para lançar uma despesa operacional.</SheetDescription>
        </SheetHeader>
        <div className="flex-1 overflow-y-auto py-4">
          <ExpenseForm key={formKey} onSuccess={handleSuccess} />
        </div>
      </SheetContent>
    </Sheet>
  )
}
