'use client'

import { useActionState, useState } from 'react'
import { Loader2, CheckCircle2, AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { saveFiscalProfileAction } from '@/app/actions/reports'
import type { FiscalProfile } from '@/types'

const TAX_REGIMES = [
  { value: 'SIMPLES_NACIONAL', label: 'Simples Nacional' },
  { value: 'LUCRO_PRESUMIDO',  label: 'Lucro Presumido' },
  { value: 'LUCRO_REAL',       label: 'Lucro Real' },
  { value: 'MEI',              label: 'MEI' },
]

interface Props { profile: FiscalProfile | null }

export function FiscalProfileForm({ profile }: Props) {
  const [state, formAction, isPending] = useActionState(saveFiscalProfileAction, null)
  const [rate, setRate] = useState(String(profile?.estimated_tax_rate ?? ''))

  return (
    <form action={formAction} className="space-y-4">
      {state?.success === false && (
        <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state?.success === true && (
        <div className="flex items-start gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2.5 text-sm text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
          <span>Perfil fiscal salvo com sucesso.</span>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="tax_regime">Regime Tributário</Label>
          <select
            id="tax_regime"
            name="tax_regime"
            defaultValue={profile?.tax_regime ?? 'SIMPLES_NACIONAL'}
            disabled={isPending}
            className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
          >
            {TAX_REGIMES.map((r) => (
              <option key={r.value} value={r.value}>{r.label}</option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="estimated_tax_rate">Alíquota Estimada (%)</Label>
          <Input
            id="estimated_tax_rate"
            name="estimated_tax_rate"
            type="number"
            step="0.1"
            min="0"
            max="100"
            value={rate}
            onChange={(e) => setRate(e.target.value)}
            placeholder="7.0"
            disabled={isPending}
          />
        </div>
      </div>

      <Button type="submit" size="sm" disabled={isPending}>
        {isPending ? <><Loader2 className="size-4 animate-spin" />Salvando...</> : 'Salvar perfil fiscal'}
      </Button>
    </form>
  )
}
