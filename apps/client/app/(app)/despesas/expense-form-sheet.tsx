'use client'

import { useCallback, useState } from 'react'
import { Plus } from 'lucide-react'
import { cn } from '@/lib/utils'
import { buttonVariants } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { ExpenseForm } from './expense-form'

export function ExpenseFormSheet() {
  const [open, setOpen] = useState(false)
  const [formKey, setFormKey] = useState(0)

  function handleOpenChange(isOpen: boolean) {
    setOpen(isOpen)
    if (!isOpen) setFormKey((k) => k + 1)
  }

  const handleSuccess = useCallback(() => setOpen(false), [])

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
