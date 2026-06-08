'use client'

import { useActionState, useState } from 'react'
import { AlertCircle, Building2, CheckCircle2, Loader2, MapPin, Search } from 'lucide-react'
import { lookupCompanyByCnpjAction } from '@/features/auth/server/actions'
import { saveFiscalProfileAction } from '@/features/reports/server/actions'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import type { CompanyLookup, FiscalProfile } from '@/shared/types'

const TAX_REGIMES = [
  { value: 'SIMPLES_NACIONAL', label: 'Simples Nacional' },
  { value: 'LUCRO_PRESUMIDO', label: 'Lucro Presumido' },
  { value: 'LUCRO_REAL', label: 'Lucro Real' },
]

interface Props {
  profile: FiscalProfile | null
  readOnly?: boolean
}

export function FiscalProfileForm({ profile, readOnly = false }: Props) {
  const [state, formAction, isPending] = useActionState(saveFiscalProfileAction, null)
  const [rate, setRate] = useState(formatRateInput(profile?.estimated_tax_rate))
  const [cnpj, setCnpj] = useState('')
  const [company, setCompany] = useState<CompanyLookup | null>(null)
  const [lookupError, setLookupError] = useState<string | null>(null)
  const [isLookupPending, setIsLookupPending] = useState(false)

  async function handleLookupCnpj() {
    setLookupError(null)
    setIsLookupPending(true)
    try {
      const result = await lookupCompanyByCnpjAction(cnpj)
      if (result.error) {
        setCompany(null)
        setLookupError(result.error)
        return
      }
      if (!result.data) {
        setCompany(null)
        setLookupError('Nao foi possivel consultar o CNPJ agora.')
        return
      }

      setCompany(result.data)
      setCnpj(formatCnpj(result.data.cnpj || cnpj))
    } finally {
      setIsLookupPending(false)
    }
  }

  const cnpjReady = cnpj.replace(/\D/g, '').length === 14

  return (
    <form action={formAction} className="space-y-4">
      {readOnly && <ReadOnlyLock />}
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

      <div className="space-y-1.5">
        <Label htmlFor="cnpj_lookup">CNPJ</Label>
        <div className="flex gap-2">
          <Input
            id="cnpj_lookup"
            type="text"
            inputMode="numeric"
            placeholder="00.000.000/0000-00"
            value={cnpj}
            onChange={(event) => {
              setCnpj(formatCnpj(event.target.value))
              setLookupError(null)
            }}
            disabled={readOnly || isPending}
            className="min-w-0"
          />
          <Button
            type="button"
            variant="outline"
            size="icon"
            onClick={handleLookupCnpj}
            disabled={readOnly || isPending || isLookupPending || !cnpjReady}
            aria-label="Buscar CNPJ"
          >
            {isLookupPending ? <Loader2 className="size-4 animate-spin" /> : <Search className="size-4" />}
          </Button>
        </div>
        {lookupError && <p className="text-xs text-destructive">{lookupError}</p>}
      </div>

      {company && (
        <div className="space-y-2 rounded-lg border bg-muted/30 p-3 text-xs text-muted-foreground">
          <div className="flex items-start gap-2">
            <Building2 className="mt-0.5 size-4 shrink-0 text-foreground" />
            <div className="min-w-0">
              <p className="truncate font-medium text-foreground">{company.legalName}</p>
              {company.tradeName && <p>{company.tradeName}</p>}
              {company.registrationStatus && <p>Situacao: {company.registrationStatus}</p>}
              {company.cnaeDescription && (
                <p>{company.cnaeCode ? `${company.cnaeCode} - ` : ''}{company.cnaeDescription}</p>
              )}
            </div>
          </div>
          {(company.addressStreet || company.addressCity || company.addressState) && (
            <div className="flex items-start gap-2">
              <MapPin className="mt-0.5 size-4 shrink-0 text-foreground" />
              <p>{formatAddress(company)}</p>
            </div>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="tax_regime">Regime Tributario</Label>
          <select
            id="tax_regime"
            name="tax_regime"
            defaultValue={profile?.tax_regime ?? 'SIMPLES_NACIONAL'}
            disabled={readOnly || isPending}
            className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
          >
            {TAX_REGIMES.map((regime) => (
              <option key={regime.value} value={regime.value}>{regime.label}</option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="estimated_tax_rate">Aliquota manual (%)</Label>
          <Input
            id="estimated_tax_rate"
            name="estimated_tax_rate"
            type="number"
            step="0.1"
            min="0"
            max="100"
            value={rate}
            onChange={(event) => setRate(event.target.value)}
            placeholder="7.0"
            disabled={readOnly || isPending}
          />
        </div>
      </div>

      <Button type="submit" size="sm" disabled={readOnly || isPending}>
        {isPending ? <><Loader2 className="size-4 animate-spin" />Salvando...</> : 'Salvar perfil fiscal'}
      </Button>
    </form>
  )
}

function formatCnpj(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 14)
  return digits
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2')
}

function formatAddress(company: CompanyLookup): string {
  const street = [company.addressStreet, company.addressNumber].filter(Boolean).join(', ')
  const city = [company.addressCity, company.addressState].filter(Boolean).join(' - ')
  return [street, company.addressNeighborhood, city, company.addressZipCode].filter(Boolean).join(' | ')
}

function formatRateInput(rate?: number): string {
  if (typeof rate !== 'number' || !Number.isFinite(rate)) return ''
  return String(Math.round(rate * 10000) / 100)
}
