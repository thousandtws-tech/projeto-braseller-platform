'use client'

import { useActionState } from 'react'
import { Plus, Loader2, CheckCircle2, AlertCircle } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { upsertStockItemAction } from '@/features/stock/server/actions'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'

export function StockItemForm({ readOnly = false }: { readOnly?: boolean }) {
  const [state, action, isPending] = useActionState(upsertStockItemAction, null)
  const isSuccess = state?.success === true
  const disabled = readOnly || isPending

  return (
    <form action={action} className="space-y-3">
      {readOnly && <ReadOnlyLock />}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="sku">SKU</Label>
          <Input id="sku" name="sku" placeholder="COD-001" required disabled={disabled} autoComplete="off" />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="description">Descrição</Label>
          <Input id="description" name="description" placeholder="Nome do produto" disabled={disabled} autoComplete="off" />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="unit_cost">Custo Unitário (R$)</Label>
          <Input id="unit_cost" name="unit_cost" type="number" step="0.01" min="0" placeholder="0,00" required disabled={disabled} />
        </div>
      </div>

      {state?.success === false && (
        <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {isSuccess && (
        <div className="flex items-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2.5 text-sm text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
          <span>Produto salvo com sucesso.</span>
        </div>
      )}

      <Button type="submit" size="sm" disabled={disabled || isSuccess}>
        {isPending ? <Loader2 className="size-4 animate-spin" /> : <Plus className="size-4" />}
        Salvar produto
      </Button>
    </form>
  )
}
