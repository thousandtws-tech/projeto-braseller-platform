'use client'

import { useActionState, useEffect, useRef } from 'react'
import { AlertCircle, CheckCircle2, Loader2, Wallet } from 'lucide-react'
import { createProfitDistributionAction } from '@/features/reports/server/actions'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import { formatCurrency } from '@/shared/api/gateway'

interface ProfitDistributionFormProps {
  periodMonth: string
  availableProfit: number
  disabled?: boolean
  readOnly?: boolean
}

export function ProfitDistributionForm({
  periodMonth,
  availableProfit,
  disabled = false,
  readOnly = false,
}: ProfitDistributionFormProps) {
  const [state, formAction, isPending] = useActionState(createProfitDistributionAction, null)
  const formRef = useRef<HTMLFormElement>(null)
  const today = new Date().toISOString().split('T')[0]
  const canSubmit = !disabled && availableProfit > 0

  useEffect(() => {
    if (state?.success !== true) return
    formRef.current?.reset()
  }, [state])

  return (
    <form ref={formRef} action={formAction} className="space-y-3">
      <input type="hidden" name="period_month" value={periodMonth} />
      {readOnly && <ReadOnlyLock />}

      {state?.success === false && (
        <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2 text-xs text-destructive">
          <AlertCircle className="size-3.5 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state?.success === true && (
        <div className="flex items-start gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2 text-xs text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-3.5 mt-0.5 shrink-0" />
          <span>Retirada de {formatCurrency(state.amount)} registrada.</span>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label htmlFor="profit_amount" className="text-xs">Valor (R$)</Label>
          <Input
            id="profit_amount"
            name="amount"
            type="number"
            step="0.01"
            min="0.01"
            max={availableProfit > 0 ? availableProfit : undefined}
            placeholder="0,00"
            required
            disabled={!canSubmit || isPending}
            className="h-8 text-xs"
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="distributed_at" className="text-xs">Data</Label>
          <Input
            id="distributed_at"
            name="distributed_at"
            type="date"
            defaultValue={today}
            required
            disabled={!canSubmit || isPending}
            className="h-8 text-xs"
          />
        </div>
      </div>

      <div className="space-y-1">
        <Label htmlFor="recipient_name" className="text-xs">Destino</Label>
        <Input
          id="recipient_name"
          name="recipient_name"
          type="text"
          placeholder="Socio, administrador ou conta destino"
          disabled={!canSubmit || isPending}
          className="h-8 text-xs"
        />
      </div>

      <div className="space-y-1">
        <Label htmlFor="profit_notes" className="text-xs">Observacao</Label>
        <Input
          id="profit_notes"
          name="notes"
          type="text"
          placeholder="Ex: retirada mensal"
          disabled={!canSubmit || isPending}
          className="h-8 text-xs"
        />
      </div>

      <Button type="submit" size="sm" className="w-full" disabled={!canSubmit || isPending}>
        {isPending
          ? <><Loader2 className="size-3.5 animate-spin" />Registrando...</>
          : <><Wallet className="size-3.5" />Registrar retirada</>
        }
      </Button>
    </form>
  )
}
