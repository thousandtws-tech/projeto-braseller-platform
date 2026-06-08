'use client'

import { useActionState } from 'react'
import { AlertCircle, CheckCircle2, FileSignature, Loader2 } from 'lucide-react'
import { batchSignAccountingClosingsAction } from '@/features/reports/server/actions'
import { Button } from '@/shared/ui/button'

interface BatchClosingActionProps {
  periodMonth: string
  tenantIds: string[]
}

export function BatchClosingAction({ periodMonth, tenantIds }: BatchClosingActionProps) {
  const [state, action, isPending] = useActionState(batchSignAccountingClosingsAction, null)
  const disabled = tenantIds.length === 0 || isPending

  return (
    <form action={action} className="flex flex-col gap-3 rounded-lg border border-border bg-muted/25 p-4 sm:flex-row sm:items-center sm:justify-between">
      <input type="hidden" name="period_month" value={periodMonth} />
      {tenantIds.map((tenantId) => (
        <input key={tenantId} type="hidden" name="tenant_ids" value={tenantId} />
      ))}

      <div className="space-y-1">
        <p className="text-sm font-medium">Fechamento BPO em lote</p>
        <p className="text-xs text-muted-foreground">
          {tenantIds.length > 0
            ? `${tenantIds.length} clientes pendentes prontos para assinatura.`
            : 'Nenhum cliente pendente pronto para assinatura.'}
        </p>
        {state?.success === false && (
          <p className="flex items-center gap-1.5 text-xs text-destructive">
            <AlertCircle className="size-3.5" />
            {state.error}
          </p>
        )}
        {state?.success === true && (
          <p className="flex items-center gap-1.5 text-xs text-emerald-700 dark:text-emerald-400">
            <CheckCircle2 className="size-3.5" />
            {state.data.signed_count} assinados, {state.data.skipped_count} ignorados, {state.data.failed_count} falhas.
          </p>
        )}
      </div>

      <Button type="submit" size="sm" disabled={disabled} className="shrink-0">
        {isPending ? <Loader2 className="size-4 animate-spin" /> : <FileSignature className="size-4" />}
        Assinar pendentes
      </Button>
    </form>
  )
}
