'use client'

import { useActionState, useEffect, useRef, useState } from 'react'
import { Loader2, AlertCircle, CheckCircle2, Paperclip, X } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { DatePicker } from '@/shared/ui/date-picker'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select'
import { createExpenseAction } from '@/features/reports/server/actions'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

const CATEGORY_VALUES = ['OPERATIONAL', 'PACKAGING', 'SUPPLIES', 'LABOR', 'BANK_FEE', 'SHIPPING', 'TAX', 'OTHER'] as const

interface ExpenseFormProps {
  onSuccess?: (expenseDate?: string) => void
  dict: Dictionary
}

export function ExpenseForm({ onSuccess, dict }: ExpenseFormProps) {
  const [state, formAction, isPending] = useActionState(createExpenseAction, null)
  const [fileName, setFileName] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const formRef = useRef<HTMLFormElement>(null)

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    setFileName(e.target.files?.[0]?.name ?? null)
  }

  function clearFile() {
    setFileName(null)
    if (fileRef.current) fileRef.current.value = ''
  }

  useEffect(() => {
    if (state?.success !== true) return
    formRef.current?.reset()
    const timer = window.setTimeout(() => {
      setFileName(null)
      onSuccess?.(state.expenseDate)
    }, 800)
    return () => window.clearTimeout(timer)
  }, [state, onSuccess])

  return (
    <form ref={formRef} action={formAction} className="space-y-3">
      {state?.success === false && (
        <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2 text-xs text-destructive">
          <AlertCircle className="size-3.5 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state?.success === true && (
        <div className="flex items-start gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2 text-xs text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-3.5 mt-0.5 shrink-0" />
          <span>{dict.expenses.form.success}</span>
        </div>
      )}

      <div className="space-y-1">
        <Label htmlFor="expense_date" className="text-xs">{dict.expenses.form.fields.date}</Label>
        <DatePicker
          id="expense_date"
          name="expense_date"
          defaultValue={new Date().toISOString().split('T')[0]}
          disabled={isPending}
          buttonClassName="h-8 text-xs"
        />
      </div>

      <div className="space-y-1">
        <Label htmlFor="description" className="text-xs">{dict.expenses.form.fields.description}</Label>
        <Input
          id="description"
          name="description"
          type="text"
          placeholder={dict.expenses.form.fields.descriptionPlaceholder}
          required
          disabled={isPending}
          className="h-8 text-xs"
        />
      </div>

      <div className="space-y-1">
        <Label htmlFor="category" className="text-xs">{dict.expenses.form.fields.category}</Label>
        <Select
          name="category"
          defaultValue=""
          required
          disabled={isPending}
        >
          <SelectTrigger id="category" className="h-8 w-full text-xs">
            <SelectValue placeholder={dict.expenses.form.fields.categoryPlaceholder} />
          </SelectTrigger>
          <SelectContent align="start">
            <SelectGroup>
              <SelectItem value="">{dict.expenses.form.fields.categoryPlaceholder}</SelectItem>
              {CATEGORY_VALUES.map((c) => (
                <SelectItem key={c} value={c}>{dict.expenses.categories[c]}</SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-1">
        <Label htmlFor="amount" className="text-xs">{dict.expenses.form.fields.value}</Label>
        <Input
          id="amount"
          name="amount"
          type="number"
          step="0.01"
          min="0.01"
          placeholder="0,00"
          required
          disabled={isPending}
          className="h-8 text-xs"
        />
      </div>

      {/* Comprovante */}
      <div className="space-y-1">
        <Label className="text-xs">{dict.expenses.form.receipt.label}</Label>
        <input
          ref={fileRef}
          name="file"
          type="file"
          accept="image/*,application/pdf"
          onChange={handleFileChange}
          disabled={isPending}
          className="hidden"
          id="expense-file"
        />
        {fileName ? (
          <div className="flex items-center gap-2 rounded-lg border border-border bg-muted/40 px-3 py-2">
            <Paperclip className="size-3.5 text-muted-foreground shrink-0" />
            <span className="text-xs truncate flex-1">{fileName}</span>
            <button type="button" onClick={clearFile} className="text-muted-foreground hover:text-destructive transition-colors">
              <X className="size-3.5" />
            </button>
          </div>
        ) : (
          <label
            htmlFor="expense-file"
            className="flex items-center gap-2 rounded-lg border border-dashed border-border px-3 py-2 text-xs text-muted-foreground hover:border-primary/50 hover:text-primary transition-colors cursor-pointer"
          >
            <Paperclip className="size-3.5 shrink-0" />
            {dict.expenses.form.receipt.attach}
          </label>
        )}
      </div>

      <Button type="submit" size="sm" className="w-full" disabled={isPending}>
        {isPending
          ? <><Loader2 className="size-3.5 animate-spin" />{dict.expenses.form.submitting}</>
          : dict.expenses.form.submit
        }
      </Button>
    </form>
  )
}
